package com.shipsense.service;

import com.shipsense.dto.HubResponse;
import com.shipsense.entity.Hub;
import com.shipsense.exception.NotFoundException;
import com.shipsense.repository.HubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    public HubService(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public List<HubResponse> findAll() {
        return hubRepository.findAll().stream()
                .map(HubResponse::from)
                .toList();
    }

    public Hub getByCodeOrThrow(String code) {
        return hubRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Hub not found: " + code));
    }
}
