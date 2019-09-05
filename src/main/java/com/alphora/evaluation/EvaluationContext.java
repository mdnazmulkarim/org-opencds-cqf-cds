package com.alphora.evaluation;

import org.cqframework.cql.elm.execution.Library;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.alphora.exceptions.NotImplementedException;
import com.alphora.hooks.Hook;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderDstu2;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderR4;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

import java.io.IOException;
import java.util.List;

public abstract class EvaluationContext<T extends IBaseResource> {

    // Provided
    private Hook hook;
    private FhirVersionEnum fhirVersion;
    private FhirContext fhirContext;
    private BaseFhirDataProvider systemProvider;
    private TerminologyProvider terminologyProvider;
    private Context context;
    private T planDefinition;
    private Library library;

    // Requires resolution
    private BaseFhirDataProvider remoteProvider;
    private List<Object> contextResources;
    private List<Object> prefetchResources;


    public EvaluationContext(Hook hook, FhirVersionEnum fhirVersion, BaseFhirDataProvider systemProvider,
                             TerminologyProvider terminologyProvider, Context context, T planDefinition)
    {
        this.hook = hook;
        this.fhirVersion = fhirVersion;
        this.fhirContext = new FhirContext(fhirVersion);
        this.systemProvider = systemProvider;
        this.terminologyProvider = terminologyProvider;
        this.context = context;
        this.planDefinition = planDefinition;
    }

    public Hook getHook() {
        return hook;
    }

    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public BaseFhirDataProvider getSystemProvider() {
        return systemProvider;
    }

    public T getPlanDefinition() {
        if (planDefinition == null) {
            throw new RuntimeException("Provided PlanDefinition cannot be null");
        }
        return planDefinition;
    }

    public Library getLibrary() {
        if (library == null) {
            throw new RuntimeException("Provided Library cannot be null");
        }
        return library;
    }

    public BaseFhirDataProvider getRemoteProvider() {
        if (remoteProvider == null) {
            if (hook.getRequest().getFhirServerUrl() != null) {
                switch (fhirVersion) {
                    case DSTU2:
                        remoteProvider = new FhirDataProviderDstu2().setEndpoint(hook.getRequest().getFhirServerUrl());
                        break;
                    case DSTU3:
                        remoteProvider = new FhirDataProviderStu3().setEndpoint(hook.getRequest().getFhirServerUrl());
                        break;
                    case R4:
                        remoteProvider = new FhirDataProviderR4().setEndpoint(hook.getRequest().getFhirServerUrl());
                        break;
                    default:
                        throw new NotImplementedException("This CDS Hooks implementation is not configured for FHIR version: " + fhirVersion.getFhirVersionString());
                }
                remoteProvider.setTerminologyProvider(terminologyProvider);
            }
            if (hook.getRequest().getFhirAuthorization() != null
                    && hook.getRequest().getFhirAuthorization().getTokenType().equals("Bearer"))
            {
                BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(hook.getRequest().getFhirAuthorization().getAccessToken());
                remoteProvider.getFhirClient().registerInterceptor(authInterceptor);

                // TODO: account for the expires_in, scope and subject properties within workflow
            }
        }
        return remoteProvider;
    }

    public Context getContext() {
        if (context == null) {
            throw new RuntimeException("The cql execution context must be provided");
        }
        return context;
    }

    public List<Object> getContextResources() {
        if (contextResources == null) {
            contextResources = EvaluationHelper.resolveContextResources(hook.getContextResources(), fhirContext);
            if (hook.getRequest().isApplyCql()) {
                applyCqlToResources(contextResources);
            }
        }
        return contextResources;
    }

    public List<Object> getPrefetchResources() throws IOException {
        if (prefetchResources == null) {
            prefetchResources =
                    EvaluationHelper.resolvePrefetchResources(
                            hook,
                            fhirContext,
                            getRemoteProvider() == null ? systemProvider.getFhirClient() : getRemoteProvider().getFhirClient()
                    );
            if (hook.getRequest().isApplyCql()) {
                applyCqlToResources(prefetchResources);
            }
        }
        return prefetchResources;
    }

    // NOTE: This is an operation defined in the cqf-ruler
    abstract void applyCqlToResources(List<Object> resources);
}