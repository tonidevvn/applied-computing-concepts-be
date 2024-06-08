package com.scraper.api.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.scraper.api.config.ScraperConfig;
import com.scraper.api.model.MainPage;
import com.scraper.api.model.ProductData;
import com.scraper.api.untils.WebDriverHelper;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    private static final ScraperConfig scraperConfig = new ScraperConfig();
    private static WebDriver driver;
    private static WebDriverHelper webDriverHelper;

    //Reading data from property file to a list
    @Value("#{'${website.urls}'.split(',')}")
    List<String> urls;

    private void webDriverInit() {
        if (driver == null) {
            driver = scraperConfig.setupWebDriver(true);
            WebDriverHelper.init(driver);
        }
    }

    private  void webDriverRelease() {
        WebDriverHelper.shutDownScraper();
        driver = null;
    }

    @Override
    public Set<ProductData> getProducts() {
        // init web driver
        webDriverInit();
        String filePath = "./data/products.csv";

        Set<ProductData> responseProducts = new HashSet<>();
            List<ProductData> products = new ArrayList<>();
            try (CSVReader csvReader = new CSVReader(new FileReader(Paths.get(filePath).toFile()))) {
                List<String[]> records = csvReader.readAll();
                records.remove(0); // Remove header row

                for (String[] record : records) {
                    ProductData responseProduct = new ProductData();
                    if (record.length == 6) {
                        responseProduct.setName(record[1]);
                        responseProduct.setBrand(record[2]);
                        responseProduct.setPrice(record[3]);
                        responseProduct.setImage(record[4]);
                        responseProduct.setUrl(record[5]);
                        if (responseProduct.getName() != null)
                            responseProducts.add(responseProduct);
                    }
                }
            } catch (IOException | CsvException e) {
                e.printStackTrace();
            }

        return responseProducts;
    }

    @Override
    public Set<ProductData> getProductsByKeyword(String keyword) {
        // init web driver
        webDriverInit();

        Set<ProductData> responseProducts = new HashSet<>();
        //Traversing through the urls
        for (String url: urls) {
            String fullUrl = url + keyword;
            driver.get(fullUrl);
            WebDriverHelper.waitInSeconds(10);

            if (url.contains("zehrs")) {
                extractDataFromZehrs(responseProducts);
            }
        }
        // release web driver
        webDriverRelease();

        return responseProducts;
    }

    private void extractDataFromZehrs(Set<ProductData> responseProducts) {
        try {
            // Find product elements
            MainPage mainPage = new MainPage(driver);

            String csvFile = "data/products.csv";
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile));
            String[] header = {"No", "Product Name", "Brand", "Price", "Image URL", "Product URL"};
            writer.writeNext(header);

            int count = 1;
            int limitCheck = 1;
            List<WebElement> searchProducts = mainPage.searchProducts;
            WebDriverHelper.waitInSeconds(10);
            for (WebElement product: searchProducts) {
                ProductData responseProduct = new ProductData();

                try {
                    WebElement we = product.findElement(By.cssSelector("span.product-name__item--name"));
                    if (we != null && !StringUtils.isEmpty(we.getText()) ) {
                        responseProduct.setName(we.getText());
                    }

                    we = product.findElement(By.cssSelector("span.product-name__item--brand"));
                    if (we != null && !StringUtils.isEmpty(we.getText()) ) {
                        responseProduct.setBrand(we.getText());
                    }

                    try {
                        we = product.findElement(By.cssSelector("span.selling-price-list__item__price--now-price__value"));
                    } catch (NoSuchElementException e) {
                        try {
                            we = product.findElement(By.cssSelector("span.selling-price-list__item__price--sale__value"));
                        }  catch (NoSuchElementException e2) {
                            // do nothing
                        }
                    }
                    assert we != null;
                    responseProduct.setPrice(we.getText());

                    we = product.findElement(By.className("responsive-image--product-tile-image"));
                    if (we != null && !StringUtils.isEmpty(we.getAttribute("src"))) {
                        responseProduct.setImage(we.getAttribute("src"));
                    }

                    we = product.findElement(By.className("product-tile__details__info__name__link"));
                    if (we != null && !StringUtils.isEmpty(we.getAttribute("href"))) {
                        responseProduct.setUrl(we.getAttribute("href"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (responseProduct.getName() != null) {
                    // && responseProduct.getBrand() != null && responseProduct.getPrice() != null && responseProduct.getImage() != null && responseProduct.getUrl() != null) {
                    String[] data = { String.valueOf(count++), responseProduct.getName(), responseProduct.getBrand(), responseProduct.getPrice(), responseProduct.getImage(), responseProduct.getUrl() };
                    writer.writeNext(data);
                    if (responseProduct.getName() != null) responseProducts.add(responseProduct);
                    limitCheck++;
                    if (limitCheck > 10) {
                        break;
                    }
                }

            }
            // Close CSV writer and browser
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
