package n3phele.factory.test.integration;
import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.ws.rs.core.UriBuilder;

import n3phele.security.EncryptedAWSCredentials;
import n3phele.service.model.core.ExecutionFactoryCreateRequest;
import n3phele.service.model.core.NameValue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;


public class VirtualServerResourceTest {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}


	@Before
	public void setUp() throws Exception {
		client = Client.create();
		
		client.setConnectTimeout(60000);
		client.setReadTimeout(60000);
		
		//load all properties from the crendentials.properties file where sensible credentials are registered for tests
		try
		{
			/* 
			 * Create this file, contains sensitive data, must not be pushed to repo (delete after running tests)
			 * Get testAccessId and testAccessKey from EC2 Console -> Security Credentials -> Access keys
			 * You can create a new pair of id/key since the key can only be seen once
			 */
			testResource = new TestResource("n3phele.factory.test.integration.credentials");
		}
		catch(FileNotFoundException e)
		{			
			throw new FileNotFoundException("The necessary file with test credentials was not found. Manually create the file and put real credentials there so integration tests can reach the cloud. See tests for necessary variables.");
		}		

		client.addFilter(new HTTPBasicAuthFilter(testResource.get("factoryUser", ""), testResource.get("factorySecret", "")));
		
//		String serverAddress = testResource.get("testServerAddress", "http://ec2factory2.appspot.com/");
		String serverAddress = testResource.get("testServerAddress", "http://localhost:8888");
		webResource = client.resource(UriBuilder.fromUri(serverAddress + "/resources/virtualServer").build());
	}

	private Client client;
	private WebResource webResource;
	private TestResource testResource;
	
	@Test
	public void testCreateVM() throws UnsupportedEncodingException, NoSuchAlgorithmException, URISyntaxException {
		WebResource resource =  webResource.path("/");

		String accessId = EncryptedAWSCredentials.encryptX(testResource.get("testAccessId", ""), testResource.get("factorySecret", ""));
		String secret = EncryptedAWSCredentials.encryptX(testResource.get("testAccessKey", ""), testResource.get("factorySecret", ""));
		
		ExecutionFactoryCreateRequest request = new ExecutionFactoryCreateRequest();
		
		request.accessKey = accessId;
		request.encryptedSecret = secret;
		request.location = new URI("https://ec2.amazonaws.com");
		request.description = "description";
		request.name = "vm_name";
		request.owner = new URI("http://localhost/");

		// Change this, must be unique value
		request.idempotencyKey = "idempotencyKey12345";
		ArrayList<NameValue> parameters = new ArrayList<NameValue>();
		parameters.add(new NameValue("nodeCount", "1"));
		parameters.add(new NameValue("imageId", "ami-bba18dd2"));
		parameters.add(new NameValue("instanceType", "t1.micro"));
		parameters.add(new NameValue("security_groups", "default_lis"));
		parameters.add(new NameValue("key_name", "lis_key"));
		parameters.add(new NameValue("user_data", ""));
		request.parameters = parameters;

		ClientResponse result = resource.post(ClientResponse.class, request);
		assertEquals(201, result.getStatus());
	}

}
