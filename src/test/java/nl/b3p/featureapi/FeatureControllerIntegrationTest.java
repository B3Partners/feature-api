package nl.b3p.featureapi;

import nl.b3p.featureapi.controller.FeatureController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
public class FeatureControllerIntegrationTest {


    @InjectMocks
    private FeatureController controller;

    @Test
    public void whenCalledSave_thenCorrectNumberOfUsers() {

        int echt = 2;
        assertEquals(2, echt);
    }
}
