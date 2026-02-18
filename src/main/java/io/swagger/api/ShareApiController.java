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
import org.epos.dbconnector.util.JsonUtil;
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
        
        // Get the configuration value - decrypt if it comes encrypted
        String configValue = body.getConfiguration();
        if (AESUtil.isEncrypted(configValue)) {
            log.info("Configuration is encrypted, will re-encrypt with fixed salt for storage");
            configValue = AESUtil.decrypt(configValue);
        }
        
        // Always store encrypted with fixed salt for deterministic storage
        String encryptedValue = AESUtil.encryptDeterministic(configValue);
        Configuration configuration = new Configuration(key, encryptedValue);

        ConfigurationMethod.saveConfiguration(configuration);
        
        KeyCreated keyCreated = new KeyCreated();
        keyCreated.setKey(key);
        
        return ResponseEntity.ok(keyCreated);
    }

    public ResponseEntity<String> findConfigurationsByID(@Parameter(in = ParameterIn.PATH, description = "Status values that need to be considered for filter", required=true, schema=@Schema()) @PathVariable("instance_id") String configuration
) {
        Configuration config = ConfigurationMethod.getConfigurationById(configuration);
        
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Decrypt the stored value, then normalize for readable output
        String decryptedValue = AESUtil.decrypt(config.getConfiguration());
        String normalizedValue = JsonUtil.normalize(decryptedValue);
        
        return ResponseEntity.ok(normalizedValue);
    }

    public ResponseEntity<List<ModelConfiguration>> findAllConfigurations() {
        List<Configuration> configs = ConfigurationMethod.getConfigurations();
        
        if (configs == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        // Decrypt and normalize each configuration for readable output
        List<ModelConfiguration> normalizedConfigs = new ArrayList<>();
        for (Configuration config : configs) {
            ModelConfiguration modelConfig = new ModelConfiguration();
            modelConfig.setId(config.getId());
            String decryptedValue = AESUtil.decrypt(config.getConfiguration());
            modelConfig.setConfiguration(JsonUtil.normalize(decryptedValue));
            normalizedConfigs.add(modelConfig);
        }
        
        return ResponseEntity.ok(normalizedConfigs);
    }

    public ResponseEntity<ModelConfiguration> findConfigurationsByIDEncrypted(@Parameter(in = ParameterIn.PATH, description = "Configuration ID", required=true, schema=@Schema()) @PathVariable("instance_id") String configurationId) {
        Configuration config = ConfigurationMethod.getConfigurationById(configurationId);
        
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Return the encrypted value directly from the database
        ModelConfiguration modelConfiguration = new ModelConfiguration();
        modelConfiguration.setId(configurationId);
        modelConfiguration.setConfiguration(config.getConfiguration());
        
        return ResponseEntity.ok(modelConfiguration);
    }

    public ResponseEntity<List<ModelConfiguration>> findAllConfigurationsEncrypted() {
        List<Configuration> configs = ConfigurationMethod.getConfigurations();
        
        if (configs == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        // Return the encrypted values directly from the database
        List<ModelConfiguration> encryptedConfigs = new ArrayList<>();
        for (Configuration config : configs) {
            ModelConfiguration modelConfig = new ModelConfiguration();
            modelConfig.setId(config.getId());
            modelConfig.setConfiguration(config.getConfiguration());
            encryptedConfigs.add(modelConfig);
        }
        
        return ResponseEntity.ok(encryptedConfigs);
    }

    public ResponseEntity<ModelConfiguration> updateConfiguration(
            @Parameter(in = ParameterIn.PATH, description = "Configuration ID", required=true, schema=@Schema()) @PathVariable("instance_id") String configurationId,
            @Parameter(in = ParameterIn.DEFAULT, description = "Configuration", required=true, schema=@Schema()) @Valid @RequestBody String body) {
        
        // The configurations endpoint receives normalized JSON, denormalize it for storage format
        String denormalizedValue = JsonUtil.denormalize(body, true);
        log.info("Denormalized configuration for storage");
        
        // Encrypt with fixed salt for deterministic storage
        String encryptedValue = AESUtil.encryptDeterministic(denormalizedValue);
        
        Configuration configuration = new Configuration(configurationId, encryptedValue);
        
        boolean updated = ConfigurationMethod.updateConfiguration(configuration);
        
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        
        // Return the normalized version back to the client
        ModelConfiguration modelConfiguration = new ModelConfiguration();
        modelConfiguration.setId(configurationId);
        modelConfiguration.setConfiguration(body);
        
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
