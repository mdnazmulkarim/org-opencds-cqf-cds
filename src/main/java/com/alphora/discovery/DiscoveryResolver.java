package com.alphora.discovery;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PlanDefinitionResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryResolver {

    private PlanDefinitionResourceProvider planDefinitionResourceProvider;
    private LibraryResourceProvider libraryResourceProvider;
    private BaseFhirDataProvider provider;

    public List<Discovery> getDiscoveries(FhirVersionEnum version) {
        List<Discovery> discoveries = new ArrayList<>();
        IBundleProvider bundleProvider = planDefinitionResourceProvider.getDao().search(new SearchParameterMap());
        for (IBaseResource resource : bundleProvider.getResources(0, bundleProvider.size())) {
            if (resource instanceof PlanDefinition) {
                PlanDefinition planDefinition = (PlanDefinition) resource;
                Discovery discovery = getDiscovery(planDefinition, version);
                    if (discovery == null) continue;
                    discoveries.add(discovery);
                }
            }
        return discoveries;
    }

    public Discovery getDiscovery(PlanDefinition planDefinition, FhirVersionEnum version) {
        Discovery<PlanDefinition> discovery = new Discovery<>();

        if (planDefinition.hasType()) {

            boolean isEcaRule = false;
            // Validate ECA Ruler PlanDefinition
            for (Coding typeCode : planDefinition.getType().getCoding()) {
                if (typeCode.getCode().equals("eca-rule")) isEcaRule = true;
            }

            if (isEcaRule && planDefinition.hasLibrary()) {

                Library library = libraryResourceProvider.getDao().read(new IdType(planDefinition.getLibraryFirstRep().getReference()));
                if (library == null) {
                    discovery.addItem(new DiscoveryItem().setUrl("Unable to GET " + planDefinition.getLibraryFirstRep().getReference()));
                    return discovery;
                }

                if (library.hasDataRequirement()) {

                    for (DataRequirement dataReq : library.getDataRequirement()) {
                        if (!dataReq.hasType()) continue;
                        if (dataReq.hasCodeFilter()) {
                            for (DataRequirement.DataRequirementCodeFilterComponent codeFilter : dataReq.getCodeFilter()) {
                                if (codeFilter.hasPath()) {
                                    for (RuntimeSearchParam param : provider.getFhirContext().getResourceDefinition(dataReq.getType()).getSearchParams())
                                    {
                                        String[] splitPath = param.getPath().split("\\.");
                                        if (codeFilter.getPath().equals(splitPath[splitPath.length - 1])
                                                && param.getParamType() == RestSearchParameterTypeEnum.TOKEN)
                                        {

                                        }
                                    }

                                }
                            }
                        }
                    }

                }
                else {
                    // TODO: call Library/id/$data-requirements
                }

            }
        }
        return null;
    }
}
