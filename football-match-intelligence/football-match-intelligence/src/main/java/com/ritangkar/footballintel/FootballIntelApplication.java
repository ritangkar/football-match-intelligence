package com.ritangkar.footballintel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Football Match Intelligence.
 *
 * A compact analytics engine that turns raw match results into team form,
 * head-to-head comparisons and simple, explainable analytical insights.
 *
 * All data used by this service is synthetic (see /sample-data). Nothing
 * here calls a live football data provider.
 */
@SpringBootApplication
public class FootballIntelApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballIntelApplication.class, args);
    }
}
