package io.swagger.api;

import io.swagger.model.KeyCreated;
import io.swagger.model.ModelConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.epos.dbconnector.Configuration;
import org.epos.dbconnector.ConfigurationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2024-07-01T13:36:45.483299527Z[GMT]")
@RestController
public class ShareApiController implements ShareApi {

    private static final Logger log = LoggerFactory.getLogger(ShareApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public ShareApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }


    public ResponseEntity<KeyCreated> addConfiguration(@Parameter(in = ParameterIn.DEFAULT, description = "Configuration", required=true, schema=@Schema()) @Valid @RequestBody ModelConfiguration body
) {
        String accept = request.getHeader("Accept");
        
        String key = body.getId()!=null? body.getId() : UUID.randomUUID().toString();
        
        Configuration configuration = new Configuration(key, body.getConfiguration());

        ConfigurationMethod.saveConfiguration(configuration);
        
        KeyCreated keyCreated = new KeyCreated();
        keyCreated.setKey(key);
        
		return ResponseEntity.ok(keyCreated);
    }

    public ResponseEntity<ModelConfiguration> findConfigurationsByID(@Parameter(in = ParameterIn.PATH, description = "Status values that need to be considered for filter", required=true, schema=@Schema()) @PathVariable("instance_id") String configuration
) {
        
        String configurationValue = ConfigurationMethod.getConfigurationById(configuration).getConfiguration();
        
        ModelConfiguration modelConfiguration = new ModelConfiguration();
        modelConfiguration.setConfiguration(configurationValue);
        
        return ResponseEntity.ok(modelConfiguration);
    }

    public ResponseEntity<List<Configuration>> findAllConfigurations() {
        return ResponseEntity.ok(ConfigurationMethod.getConfigurations());
    }

}
