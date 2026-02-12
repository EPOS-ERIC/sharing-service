package io.swagger.api;

import io.swagger.model.KeyCreated;
import io.swagger.model.ModelConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.epos.dbconnector.Configuration;
import org.epos.dbconnector.ConfigurationMethod;
import org.epos.dbconnector.util.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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
        
        // If the configuration is encrypted, decrypt it before storing
        String configValue = body.getConfiguration();
        if (AESUtil.isEncrypted(configValue)) {
            log.info("Configuration is encrypted, decrypting before storage");
            configValue = AESUtil.decrypt(configValue);
        }
        
        Configuration configuration = new Configuration(key, configValue);

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

    public ResponseEntity<ModelConfiguration> findConfigurationsByIDEncrypted(@Parameter(in = ParameterIn.PATH, description = "Configuration ID", required=true, schema=@Schema()) @PathVariable("instance_id") String configurationId) {
        Configuration config = ConfigurationMethod.getConfigurationById(configurationId);
        
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        String encryptedValue = AESUtil.encrypt(config.getConfiguration());
        
        ModelConfiguration modelConfiguration = new ModelConfiguration();
        modelConfiguration.setId(configurationId);
        modelConfiguration.setConfiguration(encryptedValue);
        
        return ResponseEntity.ok(modelConfiguration);
    }

    public ResponseEntity<List<ModelConfiguration>> findAllConfigurationsEncrypted() {
        List<Configuration> configs = ConfigurationMethod.getConfigurations();
        
        if (configs == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<ModelConfiguration> encryptedConfigs = new ArrayList<>();
        for (Configuration config : configs) {
            ModelConfiguration modelConfig = new ModelConfiguration();
            modelConfig.setId(config.getId());
            modelConfig.setConfiguration(AESUtil.encrypt(config.getConfiguration()));
            encryptedConfigs.add(modelConfig);
        }
        
        return ResponseEntity.ok(encryptedConfigs);
    }

    public ResponseEntity<ModelConfiguration> updateConfiguration(
            @Parameter(in = ParameterIn.PATH, description = "Configuration ID", required=true, schema=@Schema()) @PathVariable("instance_id") String configurationId,
            @Parameter(in = ParameterIn.DEFAULT, description = "Configuration", required=true, schema=@Schema()) @Valid @RequestBody ModelConfiguration body) {
        
        // If the configuration is encrypted, decrypt it before storing
        String configValue = body.getConfiguration();
        if (AESUtil.isEncrypted(configValue)) {
            log.info("Configuration is encrypted, decrypting before storage");
            configValue = AESUtil.decrypt(configValue);
        }
        
        Configuration configuration = new Configuration(configurationId, configValue);
        
        boolean updated = ConfigurationMethod.updateConfiguration(configuration);
        
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        
        ModelConfiguration modelConfiguration = new ModelConfiguration();
        modelConfiguration.setId(configurationId);
        modelConfiguration.setConfiguration(configValue);
        
        return ResponseEntity.ok(modelConfiguration);
    }

    public ResponseEntity<Void> deleteConfiguration(@Parameter(in = ParameterIn.PATH, description = "Configuration ID", required=true, schema=@Schema()) @PathVariable("instance_id") String configurationId) {
        boolean deleted = ConfigurationMethod.deleteConfiguration(configurationId);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }

}
