package org.openecomp.sdc.be.resources.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class HeatParameterValueDataTest {

	private HeatParameterValueData createTestSubject() {
		return new HeatParameterValueData();
	}

	@Test
	public void testCtor() throws Exception {
		new HeatParameterValueData(new HashMap<>());
	}
	
	@Test
	public void testGetUniqueId() throws Exception {
		HeatParameterValueData testSubject;
		Object result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.getUniqueId();
	}

	
	@Test
	public void testGetValue() throws Exception {
		HeatParameterValueData testSubject;
		String result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.getValue();
	}

	
	@Test
	public void testSetValue() throws Exception {
		HeatParameterValueData testSubject;
		String value = "";

		// default test
		testSubject = createTestSubject();
		testSubject.setValue(value);
	}

	
	@Test
	public void testSetUniqueId() throws Exception {
		HeatParameterValueData testSubject;
		String uniqueId = "";

		// default test
		testSubject = createTestSubject();
		testSubject.setUniqueId(uniqueId);
	}

	
	@Test
	public void testToGraphMap() throws Exception {
		HeatParameterValueData testSubject;
		Map<String, Object> result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.toGraphMap();
	}

	
	@Test
	public void testToString() throws Exception {
		HeatParameterValueData testSubject;
		String result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.toString();
	}
}