/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vzg.service.wordpress.model;

import java.util.List;
import java.util.Map;

public class Post {

    private int id, author;

    private MayAuthorList authors;

    private String wps_subtitle, date, modified, link;

    private PostContent title, content;

    private String layout_flexible_0_text_area, subline;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAuthor() {
        return author;
    }

    public void setAuthor(int author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }


    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public PostContent getTitle() {
        return title;
    }

    public void setTitle(PostContent title) {
        this.title = title;
    }

    public PostContent getContent() {
        return content;
    }

    public void setContent(PostContent content) {
        this.content = content;
    }

    public String getWps_subtitle() {
        return wps_subtitle;
    }

    public void setWps_subtitle(String wps_subtitle) {
        this.wps_subtitle = wps_subtitle;
    }

    @Override public String toString() {
        return "Post{" +
            "id=" + id +
            ", author=" + author +
            ", date='" + date + '\'' +
            ", modified='" + modified + '\'' +
            ", link='" + link + '\'' +
            ", title=" + title +
            ", content=" + content +
            '}';
    }

    public MayAuthorList getAuthors() {
        return authors;
    }

    public void setAuthors(MayAuthorList authors) {
        this.authors = authors;
    }

    public String getLayout_flexible_0_text_area() {
        return layout_flexible_0_text_area;
    }

    public void setLayout_flexible_0_text_area(String layout_flexible_0_text_area) {
        this.layout_flexible_0_text_area = layout_flexible_0_text_area;
    }

    public String getSubline() {
        return subline;
    }

    public void setSubline(String subline) {
        this.subline = subline;
    }
}
