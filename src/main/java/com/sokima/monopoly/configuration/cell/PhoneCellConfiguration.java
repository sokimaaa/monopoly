package com.sokima.monopoly.configuration.cell;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "monopoly.cell.phone")
public class PhoneCellConfiguration {

    public static final Integer COUNT = 2;

    private FirstCompany firstCompany = new FirstCompany();

    private SecondCompany secondCompany = new SecondCompany();

    @Getter
    @Setter
    public static class FirstCompany {
        private Long price = 1000L;

        private Long rent = 100L;

        private String businessName;
    }

    @Getter
    @Setter
    public static class SecondCompany {
        private Long price = 1100L;

        private Long rent = 110L;

        private String businessName;
    }
}
