package org.openmrs.htmlformentry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openmrs.ConditionClinicalStatus.ACTIVE;
import static org.openmrs.ConditionClinicalStatus.HISTORY_OF;
import static org.openmrs.ConditionClinicalStatus.INACTIVE;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Condition;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil2_3;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.30.xml");
	}
	
	@Test
	public void shouldRecordAndEditCondition() throws Exception {
		new RegressionTestHelper() {
			
			protected String add(String widget, int offset) {
				return "w" + (Integer.parseInt(widget.substring(1)) + offset);
			}
			
			protected String getAdditionalDetailsWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 2);
			}
			
			protected String getStatusWidget(String conceptSearchWidget, boolean wasAdditionDetails) {
				return add(conceptSearchWidget, wasAdditionDetails ? 3 : 2);
			}
			
			protected String getOnsetDateWidget(String conceptSearchWidget, boolean wasAdditionDetails) {
				return add(conceptSearchWidget, wasAdditionDetails ? 5 : 4);
			}
			
			protected String getEndDateWidget(String conceptSearchWidget, boolean wasAdditionDetails) {
				return add(conceptSearchWidget, wasAdditionDetails ? 6 : 5);
			}
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Optional Coded Condition:",
				        "Required Coded Condition:", "Optional Non-coded Condition:", "Required Non-coded Condition:",
				        "Preset Condition:" };
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// filling the optional coded condition tag
				request.addParameter(widgets.get("Optional Coded Condition:"), "Epilepsy");
				request.addParameter(widgets.get("Optional Coded Condition:") + "_hid", "3476");
				request.addParameter(getAdditionalDetailsWidget(widgets.get("Optional Coded Condition:")),
				    "Additional details");
				request.addParameter(getStatusWidget(widgets.get("Optional Coded Condition:"), true), "active");
				request.addParameter(getOnsetDateWidget(widgets.get("Optional Coded Condition:"), true), "2014-02-11");
				
				// filling the required coded condition tag
				request.addParameter(widgets.get("Required Coded Condition:"), "Indigestion");
				request.addParameter(widgets.get("Required Coded Condition:") + "_hid", "3475");
				request.addParameter(getAdditionalDetailsWidget(widgets.get("Required Coded Condition:")),
				    "Additional details");
				request.addParameter(getStatusWidget(widgets.get("Required Coded Condition:"), true), "active");
				request.addParameter(getOnsetDateWidget(widgets.get("Required Coded Condition:"), true), "2014-02-11");
				
				// filling the required non-coded condition tag
				request.addParameter(widgets.get("Required Non-coded Condition:"), "Anemia (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Required Non-coded Condition:"), false), "inactive");
				request.addParameter(getOnsetDateWidget(widgets.get("Required Non-coded Condition:"), false), "2013-02-11");
				request.addParameter(getEndDateWidget(widgets.get("Required Non-coded Condition:"), false), "2019-04-11");
				
				// filling up the preset condition tag
				request.addParameter(getStatusWidget(widgets.get("Preset Condition:"), false), "history-of");
				request.addParameter(getOnsetDateWidget(widgets.get("Preset Condition:"), false), "2014-02-11");
				request.addParameter(getEndDateWidget(widgets.get("Preset Condition:"), false), "2020-04-11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				final Map<String, Condition> conditions = results.getEncounterCreated().getConditions().stream()
				        .collect(Collectors.toMap(c -> HtmlFormEntryUtil2_3.getControlId(c), c -> c));
				
				results.assertNoErrors();
				assertThat(conditions.size(), is(4));
				
				Condition actualCondition;
				{
					actualCondition = conditions.get("optional_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Epilepsy"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Indigestion"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertEquals("2013-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2019-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("optional_preset_condition");
					Assert.assertEquals(HISTORY_OF, actualCondition.getClinicalStatus());
					assertThat(actualCondition.getCondition().getCoded().getId(), is(22));
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2020-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// removing the concept of the optional coded condition tag
				request.removeParameter(widgets.get("Optional Coded Condition:"));
				request.removeParameter(widgets.get("Optional Coded Condition:") + "_hid");
				
				// filling for the first time in EDIT mode the optional non-coded condition tag 
				request.addParameter(widgets.get("Optional Non-coded Condition:"), "Sneezy cold (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Optional Non-coded Condition:"), false), "inactive");
				request.addParameter(getOnsetDateWidget(widgets.get("Optional Non-coded Condition:"), false), "2013-02-11");
				request.addParameter(getEndDateWidget(widgets.get("Optional Non-coded Condition:"), false), "2019-04-11");
				
				// changing the onset date of the required non-coded condition tag
				request.setParameter(getOnsetDateWidget(widgets.get("Required Non-coded Condition:"), false), "2014-02-11");
				
				// removing the status of the preset condition tag
				request.removeParameter(getStatusWidget(widgets.get("Preset Condition:"), false));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				final Map<String, Condition> conditions = results.getEncounterCreated().getConditions().stream()
				        .collect(Collectors.toMap(c -> HtmlFormEntryUtil2_3.getControlId(c), c -> c));
				
				results.assertNoErrors();
				assertThat(conditions.size(), is(3));
				
				Condition actualCondition;
				{
					actualCondition = conditions.get("required_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Indigestion"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("optional_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Sneezy cold (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertEquals("2013-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2019-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2019-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
			}
			
		}.run();
	}
	
	@Test
	public void shouldInitializeValuesFromEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// Verify for condition
				assertTrue(html.contains("Condition: <span class=\"value\">Edema</span>"));
				// Verify for condition status
				assertTrue(html.contains("Status: <span class=\"value\">inactive</span>"));
				// Verify for onset date
				assertTrue(html.contains("Onset Date: <span class=\"value\">12/01/2017</span>"));
				// Verify for end date
				assertTrue(html.contains("End Date: <span class=\"value\">15/01/2019</span>"));
				
			}
			
			@Override
			public Patient getPatientToEdit() {
				return getPatient();
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// Verify the condition default value - 'Edema'
				assertTrue(html.contains(
				    "<input type=\"text\"  id=\"w30\" name=\"w30\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
				// Verify the condition status - 'Inactive'
				assertTrue(html.contains(
				    "<input type=\"radio\" id=\"w32_1\" name=\"w32\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
				// Verify the onset date - '2017-01-12'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w34-display', '#w34', '2017-01-12')</script>"));
				// Verify the end date - '2019-01-15'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w35-display', '#w35', '2019-01-15')</script>"));
				
			}
			
		}.run();
	}
	
}
