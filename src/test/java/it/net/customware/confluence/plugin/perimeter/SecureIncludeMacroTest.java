package it.net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.model.content.ContentRepresentation;
import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.PageRestrictionsDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import org.junit.Test;

import static it.net.customware.confluence.plugin.perimeter.pageobjects.SecureIncludeMacro.locateSecureIncludeMacro;
import static it.net.customware.confluence.plugin.perimeter.pageobjects.SecureIncludeMacro.locateSecureIncludeMacros;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SecureIncludeMacroTest extends DefaultStatelessTestRunner {
    @Test public void userShouldBeAbleToViewSecurePageContentIncludedByMacro() {
        String securePageContent = randomAlphanumeric(32);
        Content securePage =
                createPageForUser(UserWithDetails.ADMIN,"Secure page " + randomAlphanumeric(8),
                        securePageContent, ContentRepresentation.STORAGE);
        ViewPage viewSecurePage = product.loginAndView(UserWithDetails.ADMIN, securePage);
        PageRestrictionsDialog pageRestrictionsDialog = viewSecurePage.openToolsMenu().openPageRestrictionsDialog();
        pageRestrictionsDialog.addViewRestrictionToCurrentUser();
        pageRestrictionsDialog.saveDialog();

        String id = randomAlphanumeric(4).toLowerCase();
        Content page = createPage("Top Secret " + randomAlphanumeric(8), "{secure-include:id=" + id + "}", ContentRepresentation.WIKI);

        product.viewPage(page);

        locateSecureIncludeMacro(product, id).includePage(defaultSpace.get().getKey() + ":" + securePage.getTitle());

        product.loginAndViewRestricted(defaultUser.get(), securePage);
        assertThat(
            "User cannot view a page restricted by Admin",
            product.getTester().getDriver().getTitle(),
            containsString("No Permission")
        );

        viewPage = product.visit(ViewPage.class, page);
        String viewPageText = viewPage.getTextContent();
        assertThat(viewPageText, containsString(securePageContent));
    }

    @Test public void userShouldBeAbleToAddMultipleSecureIncludeMacros() {
        Content page = createPage("Top Secret " + randomAlphanumeric(8),
                "{secure-include:id=" + randomAlphanumeric(4).toLowerCase() + "}" +
                        "{secure-include:id=" + randomAlphanumeric(4).toLowerCase() + "}" +
                        "{secure-include:id=" + randomAlphanumeric(4).toLowerCase() + "}"
                , ContentRepresentation.WIKI);

        product.viewPage(page);

        int numberOfSecureIncludeMacros = locateSecureIncludeMacros(product).size();
        assertThat(numberOfSecureIncludeMacros, is(3));
    }
}
