package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FHIRHandler extends IncomingMessageHandler {


    public FHIRHandler(Session dataSession) {
        super(dataSession);
    }

    public void processFIHR_Event(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        PatientReported patientReported = FIHR_EventPatientReported(orgAccess,patient,immunization);
        VaccinationReported vaccinationReported = FHIR_EventVaccinationReported(orgAccess,patient,patientReported,immunization);
    }

    public PatientReported FIHR_EventPatientReported(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        PatientMaster patientMaster = null;
        PatientReported patientReported = null;
        String patientReportedExternalLink = patient.getIdentifier().get(0).getValue();

        boolean patientAlreadyExists = false;
        int levelConfidence=0;
        if(immunization != null){
            String patientReportedAuthority = immunization.getIdentifierFirstRep().getValue();
        }

        //String patientReportedType = patientReported.getPatientReportedType();
        if (StringUtils.isEmpty(patientReportedExternalLink)) {
            throw new Exception("Patient external link must be indicated");
        }


        {
            Query query = dataSession.createQuery(
                    "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
            query.setParameter(0, orgAccess.getOrg());
            query.setParameter(1, patientReportedExternalLink);
            List<PatientReported> patientReportedList = query.list();
            if (patientReportedList.size() > 0) {
            	System.err.println("patient already exists");
            	//get patient master and reported
                patientReported = patientReportedList.get(0);
                PatientHandler.patientReportedFromFhirPatient(patientReported,patient);
                patientMaster = patientReported.getPatient();

            } else { //EMPI Search matches with firstname, lastname and birthday
				List<PatientMaster> patientMasterList = PatientHandler.findMatch(dataSession,patient);
				if(patientMasterList.size() > 0){
					System.err.println("patient has a match ");
					//Create new patient reported and get existing patient master
					patientAlreadyExists = true;
					patientMaster = patientMasterList.get(0);
					levelConfidence=2;
				}else if(PatientHandler.findPossibleMatch(dataSession,patient).size()>0){
					// Found an existing patient with same firstname and lastname
					patientAlreadyExists=true;
					patientMasterList=PatientHandler.findPossibleMatch(dataSession,patient);
					patientMaster=patientMasterList.get(0);
					levelConfidence=1;
					System.err.println("patient has a possible match ");

				}else {
					//Create new patient master and patient reported
					System.err.println("patient has no match ");
					patientMaster = new PatientMaster();
					patientMaster.setOrgMaster(orgAccess.getOrg());
				}
				patientReported = new PatientReported();
				patientReported.setPatient(patientMaster);
	            PatientHandler.patientReportedFromFhirPatient(patientReported, patient);
	            patientReported.setOrgReported(orgAccess.getOrg());
				patientReported.setUpdatedDate(new Date());
			}
        }

        {
            Transaction transaction = dataSession.beginTransaction();


            dataSession.saveOrUpdate(patientMaster);
            dataSession.saveOrUpdate(patientReported);
            if(patientAlreadyExists) {
            	System.err.println("creation patientlink");
            	PatientLink pl = new PatientLink();
            	pl.setLevelConfidence(levelConfidence);
            	pl.setPatientMaster(patientMaster);
            	pl.setPatientReported(patientReported);
            	dataSession.saveOrUpdate(pl);
            }
            transaction.commit();
        }

        return patientReported;
    }

    public VaccinationReported FHIR_EventVaccinationReported(OrgAccess orgAccess, Patient patient,PatientReported patientReported, Immunization immunization) throws Exception {
		VaccinationMaster vaccinationMaster = null;
		VaccinationReported vaccinationReported = null;

		vaccinationReported = new VaccinationReported();
		vaccinationReported.setPatientReported(patientReported);
		ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,immunization);
		{
	    	Query query = dataSession.createQuery(
		    "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
	    	query.setParameter(0, patientReported);
	   		query.setParameter(1, immunization.getId());
	    	List<VaccinationReported> vaccinationReportedList = query.list();
	    	if (vaccinationReportedList.size() > 0) { // if external link found
				System.out.println("Immunization already exists");
				vaccinationMaster = vaccinationReportedList.get(0).getVaccination();
	    	} else if (false){
	    		//TODO Vacdedup find match
			} else {
				vaccinationMaster = new VaccinationMaster();
				vaccinationMaster.setPatient(patientReported.getPatient());
				vaccinationMaster.setVaccinationReported(vaccinationReported);
				vaccinationMaster.setPatient(patientReported.getPatient());
				ImmunizationHandler.vaccinationMasterFromFhirImmunization(vaccinationMaster,immunization);
			}
		}
		vaccinationReported.setVaccination(vaccinationMaster);
		if (vaccinationReported.getUpdatedDate().before(patient.getBirthDate())) {
	    	throw new Exception("Vaccination is reported as having been administered before the patient was born");
		}

	// OrgLocation
	String administeredAtLocation = immunization.getLocationTarget().getId();
	if (StringUtils.isNotEmpty(administeredAtLocation)) {
	    Query query = dataSession.createQuery(
		    "from OrgLocation where orgMaster = :orgMaster and orgFacilityCode = :orgFacilityCode");
	    query.setParameter("orgMaster", orgAccess.getOrg());
	    query.setParameter("orgFacilityCode", administeredAtLocation);
	    List<OrgLocation> orgMasterList = query.list();
	    OrgLocation orgLocation = null;
	    if (orgMasterList.size() > 0) {
			orgLocation = orgMasterList.get(0);
	    } else {
			orgLocation = new OrgLocation();
			ImmunizationHandler.orgLocationFromFhirImmunization(orgLocation, immunization);
			orgLocation.setOrgMaster(orgAccess.getOrg());
			Transaction transaction = dataSession.beginTransaction();
			dataSession.save(orgLocation);
			transaction.commit();
	    }
	    vaccinationReported.setOrgLocation(orgLocation);
	}


	Transaction transaction = dataSession.beginTransaction();
	dataSession.saveOrUpdate(vaccinationMaster);
	dataSession.saveOrUpdate(vaccinationReported);
	//dataSession.saveOrUpdate(vaccinationLink);
	transaction.commit();
	return vaccinationReported;
    }
    
    public void FHIR_EventVaccinationDeleted(OrgAccess orgAccess, String id) {
	VaccinationReported vr = new VaccinationReported();
	VaccinationMaster vm = new VaccinationMaster();

        Query query = dataSession.createQuery(
                "from  VaccinationReported where vaccinationReportedExternalLink = ?");
        query.setParameter(0, id);
        List<VaccinationReported> vaccinationReportedList = query.list();
        if (vaccinationReportedList.size() > 0) {
            vr = vaccinationReportedList.get(0);
            vm =vr.getVaccination();
        }
        Transaction transaction = dataSession.beginTransaction();

        dataSession.delete(vr);
        dataSession.delete(vm);
        transaction.commit();
    }
    
}
