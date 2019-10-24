package com.alphora.discovery;

import ca.uhn.fhir.context.FhirContext;
import org.junit.Test;

public class DiscoveryTests {

    @Test
    public void DiscoveryTest() {
        FhirContext context = FhirContext.forDstu3();
        DiscoveryResolution resolver = new DiscoveryResolution(
                context.getRestfulClientFactory().newGenericClient("http://localhost:8080/cqf-ruler/baseDstu3")
        );

        resolver.resolve();
    }
}
