package de.vzg.wis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class WordpressImporterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WordpressImporterServiceApplication.class, args);
	}

	@Scheduled(fixedRate = 1000 * 60 * 60 * 24, initialDelay = 1000 * 60 )
	public void autoImporter(){
		new WordpressAutoImporter().run();
	}

}
