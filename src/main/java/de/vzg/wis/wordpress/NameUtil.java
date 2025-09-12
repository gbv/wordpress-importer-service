package de.vzg.wis.wordpress;

import de.vzg.wis.wordpress.NameUtil.Name.Builder;
import de.vzg.wis.wordpress.NameUtil.Name.Role;
import de.vzg.wis.wordpress.model.Author;
import de.vzg.wis.wordpress.model.CoAuthor;
import de.vzg.wis.wordpress.model.MayAuthorList;
import de.vzg.wis.wordpress.model.Post;
import de.vzg.wis.wordpress.model.User;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NameUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean isParticle(String namePart) {
      return switch (namePart.toLowerCase(Locale.ROOT).replace(".", "")) {
        case "mr", "mrs", "ms", "miss", "mx", "madam", "sir", "prof", "dr", "dipl", "ing", "ma",
             "msc", "ba", "bsc" -> true;
        default -> false;
      };
    }

    public static List<Name> getAuthors(Post blogPost, String blogURL) {
        final List<Integer> authorIds =
            Optional.ofNullable(blogPost.getAuthors()).orElse(new MayAuthorList()).getAuthorIds();
        final List<String> authorNames =
            Optional.ofNullable(blogPost.getAuthors()).orElse(new MayAuthorList()).getAuthorNames();

        final List<Name> authors = new ArrayList<>();

        if (authorIds != null && !authorIds.isEmpty()) {
            Collections.reverse(authorIds);
            for (Integer authorID : authorIds) {
                final Author author;

                try {
                    author = AuthorFetcher.fetchAuthor(blogURL, authorID);
                } catch (IOException e) {
                    LOGGER.error("Error while fetching author from Blog: " + blogURL, e);
                    continue;
                }
                if (author != null && author.getName() != null) {
                    authors.add(createNameFromDisplayName(author.getName(), Role.AUTOR));
                }
            }
        } else if (authorNames != null && !authorNames.isEmpty()) {
            Collections.reverse(authorNames);
            for (String authorName : authorNames) {
                authors.add(createNameFromDisplayName(authorName, Role.AUTOR));
            }
        } else if (blogPost.getDelegate1() != null || blogPost.getDelegate2() != null
            || blogPost.getDelegate3() != null) {
            List<String> delegateAuthors =
                Stream.of(blogPost.getDelegate1(), blogPost.getDelegate2(), blogPost.getDelegate3())
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(String::isEmpty))
                    .collect(Collectors.toList());
            Collections.reverse(delegateAuthors);
            for (String authorName : delegateAuthors) {
                authors.add(createNameFromDisplayName(authorName, Role.SPEAKER));
            }
        } else {
            final User author;
            try {
                author = UserFetcher.fetchUser(blogURL, blogPost.getAuthor());
                if (author != null && author.getName() != null) {
                    authors.add(createNameFromDisplayName(author.getName(), Role.AUTOR));
                }
            } catch (IOException e) {
                LOGGER.error("Error while fetching author from Blog: " + blogURL, e);
            }
        }

        if (blogPost.getCoAuthors() != null && !blogPost.getCoAuthors().isEmpty()) {
            List<CoAuthor> coAuthors;
            try {
                coAuthors = CoAuthorFetcher.fetchCoAuthors(blogURL, blogPost.getId());
            } catch (IOException e) {
                LOGGER.error("Could not fetch co-authors!", e);
                coAuthors = new ArrayList<>();
            }
            Collections.reverse(coAuthors);
            Set<String> displayNames = authors
                .stream()
                .map(Name::display)
                .collect(Collectors.toSet());

            coAuthors.stream()
                .map(CoAuthor::getDisplay_name)
                .filter(Objects::nonNull)
                .filter(Predicate.not(displayNames::contains))
                .forEach(coAuthor -> {
                    Name coAutor = createNameFromDisplayName(coAuthor, Role.CO_AUTOR);
                    if (coAutor != null) {
                        authors.add(coAutor);
                    }
                });
        }
        return authors;
    }

    public static Name createNameFromDisplayName(String displayName, Role role) {
        String foreName = null;
        String sureName;

        Builder builder = new Builder();

        // only handles the form givenName familyName
        if (displayName.contains(" ")) {
            List<String> nameParts = Stream.of(displayName.split(" "))
                .filter(part -> !isParticle(part))
                .collect(Collectors.toList());

            for (String particle : displayName.split(" ")) {
                if (isParticle(particle)) {
                    if (builder.particle == null) {
                        builder.setParticle(particle);
                    } else {
                        builder.setParticle(builder.particle + " " + particle);
                    }
                } else {
                    break;
                }
            }

            if (nameParts.isEmpty()) {
                // only particles
                return null;
            }

            sureName = nameParts.get(nameParts.size() - 1);
            nameParts.remove(nameParts.size() - 1);

            if (!nameParts.isEmpty()) {
                foreName = String.join(" ", nameParts);
            }

            if (foreName != null && !foreName.isBlank()) {
                builder.setFirst(foreName);
            }

            if (!sureName.isBlank()) {
                builder.setLast(sureName);
            }
        }

        builder.setDisplay(displayName);
        builder.setRole(role);
        return builder.build();
    }

    public record Name(@Nullable String particle, @Nullable String first, @Nullable String last, String display,
        Role role) {

        public static enum Role {
            AUTOR,
            CO_AUTOR,
            SPEAKER;

            @Override
            public String toString() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        public static class Builder {

            private String particle = null;
            private String first = null;
            private String last = null;
            private String display = null;
            private Role role;

            public Builder setParticle(String particle) {
                this.particle = particle;
                return this;
            }

            public Builder setFirst(String first) {
                this.first = first;
                return this;
            }

            public Builder setLast(String last) {
                this.last = last;
                return this;
            }

            public Builder setDisplay(String display) {
                this.display = display;
                return this;
            }

            public Builder setRole(Role role) {
                this.role = role;
                return this;
            }

            public Name build() {
                return new Name(particle, first, last, display, role);
            }
        }
    }
}
