package io.openshift.booster;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.jayway.awaitility.Awaitility;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;

/**
 * Class to help deploy and undeploy applications to OpenShift.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenshiftTestAssistant {


	private final OpenShiftClient client;

	private final String project;

	private String applicationName;

	private final Map<String, NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean>> created
			= new LinkedHashMap<>();

	private String baseUrl;

	public OpenshiftTestAssistant() {
		this.client = new DefaultKubernetesClient().adapt(OpenShiftClient.class);
		project = client.getNamespace();
	}

	public OpenShiftClient getClient() {
		return client;
	}

	public String deployApplication() throws IOException {
		applicationName = System.getProperty("app.name");

		List<? extends HasMetadata> entities
				= deploy("application", new File("target/classes/META-INF/fabric8/openshift.yml"));

		Optional<String> first = entities.stream()
				.filter(hm -> hm instanceof DeploymentConfig)
				.map(hm -> (DeploymentConfig) hm)
				.map(dc -> dc.getMetadata().getName()).findFirst();
		if (applicationName == null && first.isPresent()) {
			applicationName = first.get();
		}

		Route route = getRoute(this.applicationName);

		this.baseUrl = "http://" + Objects.requireNonNull(route)
				.getSpec()
				.getHost();

		System.out.println("Route url: " + this.baseUrl);

		return applicationName;
	}

	private Route getRoute(String name) {
		return this.client.adapt(OpenShiftClient.class)
				.routes()
				.inNamespace(project)
				.withName(name)
				.get();
	}

	public String getRouteAsUrl(String name) {
		Route route = getRoute(name);

		return "http://" + Objects.requireNonNull(route)
				.getSpec()
				.getHost();
	}

	public void cleanup() {
		List<String> keys = new ArrayList<>(this.created.keySet());
		Collections.reverse(keys);
		for (String key : keys) {
			cleanup(key);
		}
	}

	public void cleanup(String name) {
		System.out.println("Deleting " + name);
		this.created.remove(name).delete();
	}

	public void awaitApplicationReadinessOrFail() {
		waitUntilPodIsReady();
		waitUntilRouteIsServed();
	}

	public String getBaseUrl() {

		if(null == this.baseUrl) {
			throw new IllegalStateException("'baseUrl' not resolved. You need to deploy an application first!");
		}

		return this.baseUrl;
	}

	public List<? extends HasMetadata> deploy(String name, File template) {
		try (FileInputStream fis = new FileInputStream(template)) {
			NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean> declarations
					= this.client.load(fis);
			List<HasMetadata> entities = declarations.createOrReplace();
			this.created.put(name, declarations);

			System.out.println(name + " deployed, " + entities.size() + " object(s) created.");

			return entities;
		} catch (IOException e) {
			throw new RuntimeException("Deployment failed", e);
		}
	}

	private void waitUntilPodIsReady() {
		Awaitility.await()
				.atMost(5, TimeUnit.MINUTES)
				.until(() -> this.client.pods().inNamespace(project).list().getItems().stream()
						.filter(this::isThisApplicationPod)
						.filter(this::isRunning)
						.collect(Collectors.toList())
						.size() >= 1);
		System.out.println("Pod is running");
	}

	public void waitUntilRouteIsServed() {
		Awaitility.await()
				.atMost(5, TimeUnit.MINUTES)
				.until(() -> isUrlServed(this.baseUrl));
		System.out.println("Route is served");
	}

	private boolean isRunning(Pod pod) {
		return "running".equalsIgnoreCase(pod.getStatus()
												  .getPhase());
	}

	private boolean isThisApplicationPod(Pod pod) {
		return pod.getMetadata()
				.getName()
				.startsWith(this.applicationName);
	}

	public void rolloutChanges(String name) {

		System.out.println("Rollout changes to " + name);

		client.deploymentConfigs().inNamespace(client.getNamespace())
				.withName(name).deployLatest();

		awaitDeploymentReadiness(name, false);
		System.out.println("Scaled down");

		awaitDeploymentReadiness(name, true);
		System.out.println("Scaled up");

		System.out.println(name + " is ready: "+ client.deploymentConfigs().inNamespace(client.getNamespace())
				.withName(name).isReady());
	}

	private void awaitDeploymentReadiness(String name, boolean shouldBeReady) {
		int timeout = shouldBeReady == false ? 1 : 3; // teardown faster than setup
		Awaitility.await()
						.atMost(timeout, TimeUnit.MINUTES)
						.until(() ->
							   {
								   return client.deploymentConfigs().inNamespace(client.getNamespace())
										   .withName(name).isReady()==shouldBeReady;
							   }
						);
	}

	private boolean isUrlServed(String url) {
		try {
			return RestAssured.get(url)
					.getStatusCode() < 500;
		}
		catch (Exception e) {
			return false;
		}
	}

}
