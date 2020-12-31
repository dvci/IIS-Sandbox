package org.immregistries.iis.kernal.fhir;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.logic.ImmunizationHandler;
import org.immregistries.iis.kernal.logic.PatientHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.PatientRepository;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class RestfulImmunizationProvider implements IResourceProvider {
  protected Session dataSession = null;
  protected OrgAccess orgAccess = null;
  protected OrgMaster orgMaster = null;
  protected PatientReported patientReported = null;
  protected VaccinationReported vaccinationReported = null;
  private static SessionFactory factory;

  @Override
  public Class<Immunization> getResourceType() {
    return Immunization.class;
  }

  public static Session getDataSession() {
    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory.openSession();
  }

  @Create
  public MethodOutcome createImmunization(RequestDetails theRequestDetails,
      @ResourceParam Immunization theImmunization) {
    vaccinationReported = null;
    if (theImmunization.getIdentifierFirstRep().isEmpty()) {
      throw new UnprocessableEntityException("No identifier supplied");
    }
    Session dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);
      PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession,
          theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
      if (null != pr) {
        Patient patient = PatientHandler.patientReportedToFhirPatient(pr);
        fhirHandler.processFHIR_Event(orgAccess, patient, theImmunization);
      } else {
        throw new Exception("No patient Found with the identifier supplied");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome(new IdType(theImmunization.getIdentifier().get(0).getValue()));
  }

  @Read()
  public Immunization getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
    Immunization immunization = null;
    Session dataSession = getDataSession();
    try {
      /*if (orgAccess == null) {
      authenticateOrgAccess(PARAM_USERID, PARAM_PASSWORD, PARAM_FACILITYID, dataSession);
      }*/
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);

      immunization =
          getImmunizationById(theRequestDetails, theId.getIdPart(), dataSession, orgAccess);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return immunization;
  }

  @Update()
  public MethodOutcome updateImmunization(RequestDetails theRequestDetails, @IdParam IdType theId,
      @ResourceParam Immunization theImmunization) {
    vaccinationReported = null;
    if (theImmunization.getIdentifierFirstRep().isEmpty()) {
      throw new UnprocessableEntityException("No identifier supplied");
    }
    Session dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);
      PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession,
          theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
      Patient patient = PatientHandler.patientReportedToFhirPatient(pr);

      fhirHandler.processFHIR_Event(orgAccess, patient, theImmunization);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome(new IdType(theImmunization.getIdentifier().get(0).getValue()));
  }

  @Delete()
  public MethodOutcome deleteImmunization(RequestDetails theRequestDetails, @IdParam IdType theId) {
    Session dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);
      fhirHandler.FHIR_EventVaccinationDeleted(orgAccess, theId.getIdPart());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome();
  }


  public static Immunization getImmunizationById(RequestDetails theRequestDetails, String id,
      Session dataSession, OrgAccess orgAccess) {
    Immunization immunization = null;
    VaccinationReported vaccinationReported = null;
    PatientReported patientReported = null;
    OrgLocation orgLocation = null;

    {
      Query query = dataSession
          .createQuery("from VaccinationReported where vaccinationReportedExternalLink = ?");
      query.setParameter(0, id);
      List<VaccinationReported> vaccinationReportedList = query.list();
      if (vaccinationReportedList.size() > 0) {
        vaccinationReported = vaccinationReportedList.get(0);
        immunization = ImmunizationHandler.getImmunization(theRequestDetails, vaccinationReported);
      }
    }

    /*
     * { Query query = dataSession.createQuery(
     * "from OrgLocation where  org_facility_code = ?"); query.setParameter(0,
     * vaccinationReported.getOrgLocation()); List<PatientReported>
     * patientReportedList = query.list(); if (patientReportedList.size() > 0) {
     * patientReported = patientReportedList.get(0); } }
     */
    return immunization;
  }
}