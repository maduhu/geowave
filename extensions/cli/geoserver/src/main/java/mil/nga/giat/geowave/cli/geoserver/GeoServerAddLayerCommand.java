package mil.nga.giat.geowave.cli.geoserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import mil.nga.giat.geowave.core.cli.annotations.GeowaveOperation;
import mil.nga.giat.geowave.core.cli.api.Command;
import mil.nga.giat.geowave.core.cli.api.DefaultOperation;
import mil.nga.giat.geowave.core.cli.api.OperationParams;
import mil.nga.giat.geowave.core.cli.operations.config.options.ConfigOptions;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@GeowaveOperation(name = "addlayer", parentOperation = GeoServerSection.class, restEnabled = GeowaveOperation.RestEnabledType.POST)
@Parameters(commandDescription = "Add a GeoServer layer from the given GeoWave store")
public class GeoServerAddLayerCommand extends
		DefaultOperation<String> implements
		Command
{
	private GeoServerRestClient geoserverClient = null;

	public static enum AddOption {
		ALL,
		RASTER,
		VECTOR;
	}

	@Parameter(names = {
		"-ws",
		"--workspace"
	}, required = false, description = "<workspace name>")
	private String workspace = null;

	@Parameter(names = {
		"-a",
		"--add"
	}, required = false, converter = AddOptionConverter.class, description = "For multiple layers, add (all | raster | vector)")
	private AddOption addOption = null;

	@Parameter(names = {
		"-id",
		"--adapterId"
	}, required = false, description = "select just <adapter id> from the store")
	private String adapterId = null;

	@Parameter(names = {
		"-sld",
		"--setStyle"
	}, required = false, description = "<default style sld>")
	private String style = null;

	@Parameter(description = "<GeoWave store name>")
	private List<String> parameters = new ArrayList<String>();
	private String gwStore = null;

	@Override
	public boolean prepare(
			OperationParams params ) {
		if (geoserverClient == null) {
			// Get the local config for GeoServer
			File propFile = (File) params.getContext().get(
					ConfigOptions.PROPERTIES_FILE_CONTEXT);

			GeoServerConfig config = new GeoServerConfig(
					propFile);

			// Create the rest client
			geoserverClient = new GeoServerRestClient(
					config);
		}

		// Successfully prepared
		return true;
	}

	@Override
	public void execute(
			OperationParams params )
			throws Exception {
		JCommander.getConsole().println(
				computeResults(params));
	}

	public static class AddOptionConverter implements
			IStringConverter<AddOption>
	{
		@Override
		public AddOption convert(
				final String value ) {
			AddOption convertedValue = AddOption.valueOf(value.toUpperCase());

			if (convertedValue != AddOption.ALL && convertedValue != AddOption.RASTER
					&& convertedValue != AddOption.VECTOR) {
				throw new ParameterException(
						"Value " + value + "can not be converted to an add option. " + "Available values are: "
								+ StringUtils.join(
										AddOption.values(),
										", ").toLowerCase(
										Locale.ENGLISH));
			}
			return convertedValue;
		}
	}

	@Override
	protected String computeResults(
			OperationParams params ) {
		if (parameters.size() != 1) {
			throw new ParameterException(
					"Requires argument: <store name>");
		}

		gwStore = parameters.get(0);

		if (workspace == null || workspace.isEmpty()) {
			workspace = geoserverClient.getConfig().getWorkspace();
		}

		if (addOption != null) { // add all supercedes specific adapter
									// selection
			adapterId = addOption.name();
		}

		Response addLayerResponse = geoserverClient.addLayer(
				workspace,
				gwStore,
				adapterId,
				style);

		if (addLayerResponse.getStatus() == Status.OK.getStatusCode()) {
			JSONObject jsonResponse = JSONObject.fromObject(addLayerResponse.getEntity());
			return "Add GeoServer layer for '" + gwStore + ": OK : " + jsonResponse.toString(2);
		}
		return "Error adding GeoServer layer for store '" + gwStore + "; code = " + addLayerResponse.getStatus();
	}
}
