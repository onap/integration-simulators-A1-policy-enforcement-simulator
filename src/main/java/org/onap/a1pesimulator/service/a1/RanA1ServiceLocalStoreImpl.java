package org.onap.a1pesimulator.service.a1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * A1 Service implementation which uses in-memory policy data store.
 */
@Service
public class RanA1ServiceLocalStoreImpl implements A1Service {

    private static final Logger log = LoggerFactory.getLogger(RanA1ServiceLocalStoreImpl.class);

    private Map<Integer, Map<String, String>> policyTypesMap = new HashMap<>();
    private Map<Integer, String> policySchemaMap = new HashMap<>();
    private ObjectMapper mapper;

    public RanA1ServiceLocalStoreImpl(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<String> healthCheck() throws RestClientException {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<String> putPolicySchema(Integer policyTypeId, String body) {
        policySchemaMap.put(policyTypeId, body);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> putPolicy(final Integer policyTypeId, final String policyId, final String body) {
        log.debug("Create or update policy id {} of policy type id {} with following content {} ", policyId,
                policyTypeId, body);
        if (policyTypesMap.containsKey(policyTypeId)) {
            policyTypesMap.get(policyTypeId).put(policyId, body);
        } else {
            Map<String, String> policies = new HashMap<>();
            policies.put(policyId, body);
            policyTypesMap.put(policyTypeId, policies);
        }
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<String> deletePolicy(final Integer policyTypeId, final String policyId) {
        log.debug("Delete policy id {} of policy type id {}", policyId, policyTypeId);
        if (policyTypesMap.containsKey(policyTypeId)) {
            policyTypesMap.get(policyTypeId).remove(policyId);
            return ResponseEntity.accepted().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<String> getPolicyTypeIds() throws RestClientException {
        return getRestAsString(policySchemaMap.keySet());
    }

    @Override
    public ResponseEntity<String> getPolicyType(final Integer policyTypeId) throws RestClientException {
        if (policySchemaMap.isEmpty() || !policySchemaMap.containsKey(policyTypeId)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(policySchemaMap.get(policyTypeId));
        }
    }

    @Override
    public ResponseEntity<String> getPolicyIdsOfType(final Integer policyTypeId) throws RestClientException {
        Set<String> result = new HashSet<>();
        if (policyTypesMap.containsKey(policyTypeId)) {
            result = policyTypesMap.get(policyTypeId).keySet();
        }
        return getRestAsString(result);
    }

    @Override
    public ResponseEntity<String> getPolicy(final Integer policyTypeId, final String policyId)
            throws RestClientException {
        if (policyTypesMap.containsKey(policyTypeId) && policyTypesMap.get(policyTypeId).containsKey(policyId)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                           .body(policyTypesMap.get(policyTypeId).get(policyId));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<String> getAllPoliciesForType(final Integer policyTypeId) throws RestClientException {
        return getRestAsString(policyTypesMap.get(policyTypeId));
    }

    private ResponseEntity<String> getRestAsString(Object obj) throws RestClientException {
        try {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize object", e);
        }
    }

}
