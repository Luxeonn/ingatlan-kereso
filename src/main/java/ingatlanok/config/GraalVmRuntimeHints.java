package ingatlanok.config;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.dto.PriceInfoDto;
import ingatlanok.dto.PropertyDetailsDto;
import ingatlanok.model.*;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.SidecarResponse;
import ingatlanok.scraper.SidecarResponse.SidecarItem;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM Native Image Ahead-Of-Time (AOT) reflection és resource regisztráció.
 */
@Configuration
@ImportRuntimeHints(GraalVmRuntimeHints.AppRuntimeHintsRegistrar.class)
public class GraalVmRuntimeHints {

    public static class AppRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            Class<?>[] bindingClasses = new Class<?>[] {
                    IngatlanItem.class,
                    PriceInfo.class,
                    PropertyDetails.class,
                    PortalSource.class,
                    SearchCriteria.class,
                    SearchResult.class,
                    PriceHistoryEntry.class,
                    StatusLogEntry.class,
                    ListingDto.class,
                    ListingFilterDto.class,
                    PriceInfoDto.class,
                    PropertyDetailsDto.class,
                    DashboardResponseDto.class,
                    MarketStatsDto.class,
                    PortalStatusDto.class,
                    PortalScrapeResult.class,
                    SidecarResponse.class,
                    SidecarItem.class
            };

            for (Class<?> clazz : bindingClasses) {
                hints.reflection().registerType(clazz,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.DECLARED_FIELDS);
            }

            // Statikus erőforrások és konfigurációk elérhetősége natív binárisban
            hints.resources().registerPattern("static/*");
            hints.resources().registerPattern("static/assets/*");
            hints.resources().registerPattern("application.yml");
        }
    }
}
