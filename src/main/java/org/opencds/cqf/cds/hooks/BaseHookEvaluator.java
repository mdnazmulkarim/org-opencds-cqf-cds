package org.opencds.cqf.cds.hooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.opencds.cqf.cds.evaluation.EvaluationContext;
import org.opencds.cqf.cds.providers.PrefetchDataProviderDstu2;
import org.opencds.cqf.cds.providers.PrefetchDataProviderR4;
import org.opencds.cqf.cds.providers.PrefetchDataProviderStu3;
import org.opencds.cqf.cds.response.CdsCard;
import org.cqframework.cql.elm.execution.ListTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.model.ModelResolver;
import org.opencds.cqf.cql.model.R4FhirModelResolver;
import org.opencds.cqf.cql.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.searchparam.SearchParameterResolver;

import java.io.IOException;
import java.util.List;

public abstract class BaseHookEvaluator<P extends IBaseResource> {

    public List<CdsCard> evaluate(EvaluationContext<P> context) throws IOException {

        // resolve context resources parameter
        // TODO - this will need some work for libraries with multiple parameters
        if (context.getLibrary().getParameters() != null && !(context.getHook() instanceof PatientViewHook)) {
            for (ParameterDef params : context.getLibrary().getParameters().getDef()) {
                if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                    context.getContext().setParameter(null, params.getName(), context.getContextResources());
                }
            }
        }

        // Remote data retriever
        TerminologyAwareRetrieveProvider remoteRetriever = new RestFhirRetrieveProvider(
                new SearchParameterResolver(context.getFhirContext()), context.getHookFhirClient());

        remoteRetriever.setTerminologyProvider(context.getContext().resolveTerminologyProvider());


        TerminologyAwareRetrieveProvider prefetchRetriever;
        ModelResolver resolver;
        if (context.getFhirVersion() == FhirVersionEnum.DSTU3) {
            prefetchRetriever = new PrefetchDataProviderStu3(context.getPrefetchResources(), remoteRetriever);
            resolver = new Dstu3FhirModelResolver();

        }
        else if (context.getFhirVersion() == FhirVersionEnum.DSTU2) {
            prefetchRetriever = new PrefetchDataProviderDstu2(context.getPrefetchResources(), remoteRetriever);
            resolver = new Dstu2FhirModelResolver();
        }

        else {
            prefetchRetriever = new PrefetchDataProviderR4(context.getPrefetchResources(), remoteRetriever);
            resolver = new R4FhirModelResolver();
        }

        // TODO: Get the "system" terminology provider.
        prefetchRetriever.setTerminologyProvider(context.getContext().resolveTerminologyProvider());
        context.getContext().registerDataProvider("http://hl7.org/fhir", new CompositeDataProvider(resolver, prefetchRetriever));
        context.getContext().registerTerminologyProvider(prefetchRetriever.getTerminologyProvider());

        return evaluateCdsHooksPlanDefinition(
                context.getContext(),
                context.getPlanDefinition(),
                context.getHook().getRequest().getContext().getPatientId(),
                context.getSystemFhirClient()
        );
    }

    public abstract List<CdsCard> evaluateCdsHooksPlanDefinition(Context context, P planDefinition, String patientId, IGenericClient applyClient);
}
