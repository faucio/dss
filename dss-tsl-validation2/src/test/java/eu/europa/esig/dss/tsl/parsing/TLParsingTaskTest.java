package eu.europa.esig.dss.tsl.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.spi.util.TimeDependentValues;
import eu.europa.esig.dss.tsl.dto.TrustService;
import eu.europa.esig.dss.tsl.dto.TrustServiceProvider;
import eu.europa.esig.dss.tsl.dto.TrustServiceStatusAndInformationExtensions;
import eu.europa.esig.dss.tsl.function.TrustServicePredicate;
import eu.europa.esig.dss.tsl.function.TrustServiceProviderPredicate;
import eu.europa.esig.dss.tsl.source.TLSource;
import eu.europa.esig.trustedlist.jaxb.tsl.TSPServiceType;
import eu.europa.esig.trustedlist.jaxb.tsl.TSPType;

public class TLParsingTaskTest {

	private static DSSDocument IE_TL;
	private static DSSDocument SK_TL;

	private static DSSDocument LOTL;
	private static DSSDocument LOTL_NOT_PARSEABLE;

	@BeforeAll
	public static void init() throws IOException {
		IE_TL = new FileDocument("src/test/resources/ie-tl.xml");
		SK_TL = new FileDocument("src/test/resources/sk-tl.xml");

		LOTL = new FileDocument("src/test/resources/eu-lotl.xml");
		LOTL_NOT_PARSEABLE = new FileDocument("src/test/resources/eu-lotl-not-parseable.xml");
	}

	@Test
	public void testIEDefault() {
		TLParsingTask task = new TLParsingTask(new TLSource(), IE_TL);
		TLParsingResult result = task.get();
		assertNotNull(result);
		assertEquals(5, result.getVersion());
		assertEquals(18, result.getSequenceNumber());
		assertNotNull(result.getIssueDate());
		assertNotNull(result.getNextUpdateDate());
		assertEquals("IE", result.getTerritory());
		assertNull(result.getDistributionPoints());

		List<TrustServiceProvider> trustServiceProviders = result.getTrustServiceProviders();
		assertNotNull(trustServiceProviders);
		assertEquals(3, trustServiceProviders.size());
		
		checkTSPs(trustServiceProviders);

		TrustServiceProvider postTrust = trustServiceProviders.get(0);
		assertEquals(1, postTrust.getServices().size());

		TrustServiceProvider adobe = trustServiceProviders.get(1);
		assertEquals(1, adobe.getServices().size());

		TrustServiceProvider trustPro = trustServiceProviders.get(2);
		assertEquals(2, trustPro.getServices().size());
	}

	@Test
	public void testSKDefault() {
		TLParsingTask task = new TLParsingTask(new TLSource(), SK_TL);
		TLParsingResult result = task.get();
		assertNotNull(result);
		assertEquals(5, result.getVersion());
		assertEquals(59, result.getSequenceNumber());
		assertNotNull(result.getIssueDate());
		assertNotNull(result.getNextUpdateDate());
		assertEquals("SK", result.getTerritory());
		assertNotNull(result.getDistributionPoints());

		List<TrustServiceProvider> trustServiceProviders = result.getTrustServiceProviders();
		assertNotNull(trustServiceProviders);
		assertEquals(6, trustServiceProviders.size());

		checkTSPs(trustServiceProviders);

		TrustServiceProvider nsa = trustServiceProviders.get(0);
		assertEquals(27, nsa.getServices().size());

		TrustServiceProvider disig = trustServiceProviders.get(1);
		assertEquals(56, disig.getServices().size());

		TrustServiceProvider mil = trustServiceProviders.get(2);
		assertEquals(8, mil.getServices().size());
	}

	@Test
	public void testLOTL() {
		TLParsingTask task = new TLParsingTask(new TLSource(), LOTL);
		TLParsingResult result = task.get();
		assertNotNull(result);
		assertNotNull(result);
		assertNotNull(result.getIssueDate());
		assertNotNull(result.getNextUpdateDate());
		assertEquals(5, result.getVersion());
		assertEquals(248, result.getSequenceNumber());
		assertEquals("EU", result.getTerritory());
		assertNotNull(result.getDistributionPoints());

		List<TrustServiceProvider> trustServiceProviders = result.getTrustServiceProviders();
		assertNotNull(trustServiceProviders);
		assertEquals(0, trustServiceProviders.size());
	}

	@Test
	public void notParseable() {
		TLParsingTask task = new TLParsingTask(new TLSource(), LOTL_NOT_PARSEABLE);
		DSSException exception = assertThrows(DSSException.class, () -> task.get());
		assertEquals("Unable to parse binaries", exception.getMessage());
	}

	private void checkTSPs(List<TrustServiceProvider> trustServiceProviders) {
		for (TrustServiceProvider tsp : trustServiceProviders) {

			assertNotNull(tsp.getNames());
			assertFalse(tsp.getNames().isEmpty());

			assertNotNull(tsp.getTradeNames());
			assertFalse(tsp.getTradeNames().isEmpty());

			assertNotNull(tsp.getRegistrationIdentifiers());
			assertFalse(tsp.getRegistrationIdentifiers().isEmpty());

			assertNotNull(tsp.getElectronicAddresses());
			assertFalse(tsp.getElectronicAddresses().isEmpty());

			assertNotNull(tsp.getPostalAddresses());
			assertFalse(tsp.getPostalAddresses().isEmpty());

			assertNotNull(tsp.getInformation());
			assertFalse(tsp.getInformation().isEmpty());

			assertNotNull(tsp.getServices());
			assertFalse(tsp.getServices().isEmpty());
			
			checkServices(tsp.getServices());
		}
	}

	private void checkServices(List<TrustService> services) {
		for (TrustService trustService : services) {
			assertNotNull(trustService.getCertificates());
			assertFalse(trustService.getCertificates().isEmpty());

			TimeDependentValues<TrustServiceStatusAndInformationExtensions> statusAndInformationExtensions = trustService.getStatusAndInformationExtensions();
			assertNotNull(statusAndInformationExtensions);

			TrustServiceStatusAndInformationExtensions latest = statusAndInformationExtensions.getLatest();
			assertNotNull(latest);

			assertNotNull(latest.getNames());
			assertFalse(latest.getNames().isEmpty());

			assertNotNull(latest.getStatus());
			assertNotNull(latest.getStartDate());
			assertNotNull(latest.getType());
		}
	}

	@Test
	public void testFilterAllTrustServiceProviders() {
		TLSource tlSource = new TLSource();
		tlSource.setTrustServiceProviderPredicate(new TrustServiceProviderPredicate() {

			@Override
			public boolean test(TSPType t) {
				return false;
			}
		});

		TLParsingTask task = new TLParsingTask(tlSource, IE_TL);
		TLParsingResult result = task.get();
		assertNotNull(result);
		assertEquals(0, result.getTrustServiceProviders().size());
	}

	@Test
	public void testFilterAllTrustServices() {
		TLSource tlSource = new TLSource();
		tlSource.setTrustServicePredicate(new TrustServicePredicate() {

			@Override
			public boolean test(TSPServiceType t) {
				return false;
			}

		});

		TLParsingTask task = new TLParsingTask(tlSource, IE_TL);
		TLParsingResult result = task.get();
		assertNotNull(result);
		assertEquals(0, result.getTrustServiceProviders().size());
	}

}
