package repl;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import repl.utils.SeleniumWebDriverUtils;

public class TakealotTest {

    private SeleniumWebDriverUtils driver = new SeleniumWebDriverUtils();
    private String url;
    private String testCaseName;

    @Parameters({"url"})
    @BeforeMethod
    public void setUpBeforeMethod(ITestContext context,
                                  String url) {
        this.url = url;
        testCaseName = context.getCurrentXmlTest().getName();
    }

    @Test
    public void doTest() {
        driver.startDriver(url);

        String expectedResult = "UGG Mens South Bay Sneaker Low Black - UK 9";
        //region - enter search item name
        driver.enterText("#shopfront-app > header > div > div > div.auto.cell > form > div > div.input-group-field.search-group.cell.auto > input", "css", expectedResult);
        //endregion
        //region - click search button
        driver.clickElement("#shopfront-app > header > div > div > div.auto.cell > form > div > div.input-group-button > button", "css");
        //endregion
        //region - select show all options
        driver.clickElement("//*[@id=\"60868318\"]/div/div[3]/div/a", "xpath");
        //endregion
        //region - select the size option
        driver.clickElement("#shopfront-app > div.pdp.pdp-module_pdp_1CPrg > div.grid-container.pdp-grid-container > div:nth-child(2) > div > div.pdp-main-panel > div > div > div.cell.medium-auto > div.pdp-core-module_actions_mdYzm > div.pdp-core-module_variant-selector_1bbqR > div > div > div.grid-x > div > div:nth-child(3) > button", "css");
        //endregion
        //region - add to cart
        driver.clickElement("#shopfront-app > div.pdp.pdp-module_pdp_1CPrg > div.grid-container.pdp-grid-container > div:nth-child(2) > aside > div.pdp-module_sidebar-buybox_1m6Sm > div.buybox-actions-container.buybox-module_buybox-actions_2g4b2 > div > div > div.action-cart.buybox-actions-module_button-cell_2dQyM.buybox-actions-module_add-to-cart-cell_3fXyS > a", "css");
        //endregion
        //region - go to cart
        driver.clickElement("//*[@id=\"body\"]/div[8]/div[1]/div/div/div/div/div[2]/div/div[1]/div/div[1]/div/div/div[2]/div[3]/button", "xpath");
        //endregion
        //region - get actual result and validate
        String actualResult = driver.getText("#shopfront-app > div.grid-container.cart.cart-content-module_cart_3W93Z > div.grid-x.cart-content-module_cart-container_1ucKG > section > div:nth-child(2) > div.cell.auto.small-order-2.large-order-1 > div:nth-child(1) > div > div > article > div > div > div.cell.auto > div > div:nth-child(1) > div > div.cell.small-12.medium-auto > div:nth-child(1) > div > a > h3", "css");
        Assert.assertEquals(actualResult, expectedResult);
        //endregion

        driver.shutdown();
    }
}
