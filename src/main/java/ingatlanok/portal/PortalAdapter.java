package ingatlanok.portal;

import java.util.List;

public interface PortalAdapter {

  PortalDescriptor getDescriptor();

  PortalUrlBuilder getUrlBuilder();

  List<ExtractionStrategy> getStrategies();
}
