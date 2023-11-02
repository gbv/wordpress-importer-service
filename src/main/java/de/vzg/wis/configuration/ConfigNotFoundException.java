package de.vzg.wis.configuration;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ConfigNotFoundException extends Exception {

        public ConfigNotFoundException(String message) {
            super(message);
        }
}
