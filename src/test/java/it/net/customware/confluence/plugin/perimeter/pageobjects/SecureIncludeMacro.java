package it.net.customware.confluence.plugin.perimeter.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.inject.Inject;
import org.openqa.selenium.By;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static net.customware.confluence.plugin.perimeter.SecureIncludeMacro.ID_PARAM;
import static net.customware.confluence.plugin.perimeter.SecureIncludeMacro.LINK_PARAM;

/**
 * @author yclian
 * @since 20131012
 */
public class SecureIncludeMacro extends ConfluenceAbstractPageComponent {

    public static class Locator {

        @Inject PageElementFinder finder;

        public List<SecureIncludeMacro> findAll() {

            List<SecureIncludeMacro> r = newArrayList();

            for (PageElement form: finder.findAll(By.name("secureIncludeForm"))) {
                r.add(new SecureIncludeMacro(form));
            }

            return r;
        }

        public SecureIncludeMacro find(String secureId) {
            return new SecureIncludeMacro(finder.find(By.cssSelector("#secureIncludeForm_" + secureId)));
        }
    }

    public static List<SecureIncludeMacro> locateAll(ConfluenceTestedProduct product) {
        return product.getPageBinder().bind(Locator.class).findAll();
    }

    public static SecureIncludeMacro locate(ConfluenceTestedProduct product, String secureId) {
        return product.getPageBinder().bind(Locator.class).find(secureId);
    }

    public static List<SecureIncludeMacro> locateSecureIncludeMacros(ConfluenceTestedProduct product) {
        return locateAll(product);
    }

    public static SecureIncludeMacro locateSecureIncludeMacro(ConfluenceTestedProduct product, String secureId) {
        return locate(product, secureId);
    }

    private PageElement form;
    private String id;
    private PageElement link;

    public SecureIncludeMacro(PageElement form) {

        String id = form.find(By.cssSelector("input[name=" + ID_PARAM + "]")).getAttribute("value");
        PageElement link = form.find(By.cssSelector("input[name=" + LINK_PARAM + "]"));

        this.form = form;
        this.id = id;
        this.link = link;
    }

    public PageElement includePage(String page) {
        link.type(page);
        return form.find(By.cssSelector("input[type=submit]")).click();
    }
}
