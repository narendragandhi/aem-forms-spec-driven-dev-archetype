package ${package}.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;

@Model(
    adaptables = SlingHttpServletRequest.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class Card {

    @ValueMapValue
    private String cardTitle;

    @ValueMapValue
    private String cardText;

    @ValueMapValue
    private String imagePath;

    @ValueMapValue
    private String buttonText;

    @ValueMapValue
    private String buttonLink;

    public String getCardTitle() {
        return cardTitle;
    }

    public String getCardText() {
        return cardText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getButtonText() {
        return buttonText;
    }

    public String getButtonLink() {
        return buttonLink;
    }
}
