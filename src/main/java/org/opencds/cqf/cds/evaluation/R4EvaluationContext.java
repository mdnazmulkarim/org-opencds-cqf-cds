package org.opencds.cqf.cds.evaluation;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.opencds.cqf.cds.hooks.Hook;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

import java.util.List;
import java.util.stream.Collectors;

public class R4EvaluationContext extends EvaluationContext<PlanDefinition> {


    public R4EvaluationContext(Hook hook, FhirVersionEnum fhirVersion, IGenericClient fhirClient, TerminologyProvider terminologyProvider,
                               Context context, Library library, PlanDefinition planDefinition)
    {
        super(hook, fhirVersion, fhirClient, context, library, planDefinition);
    }

    @Override
    List<Object> applyCqlToResources(List<Object> resources) {
        Bundle bundle = new Bundle();
        for (Object res : resources) {
            bundle.addEntry(new Bundle.BundleEntryComponent().setResource((Resource) res));
        }
        Parameters parameters = new Parameters();
        parameters.addParameter().setName("resourceBundle").setResource(bundle);

        Parameters ret = this.getSystemFhirClient().operation().onType(Bundle.class).named("$apply-cql").withParameters(parameters).execute();
        Bundle appliedResources = (Bundle) ret.getParameter().get(0).getResource();
        return appliedResources.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
    }
}
