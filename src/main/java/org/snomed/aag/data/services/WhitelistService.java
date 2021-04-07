package org.snomed.aag.data.services;

import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
public class WhitelistService {

	@Autowired
	private WhitelistItemRepository repository;

	public Page<WhitelistItem> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	public List<WhitelistItem> findAllByComponentIdIn(Collection<String> componentIds) {
		return repository.findAllByComponentIdIn(componentIds);
	}

	public WhitelistItem findOrThrow(String id) {
		final Optional<WhitelistItem> itemOptional = repository.findById(id);
		if (!itemOptional.isPresent()) {
			throw new NotFoundException(format("Whitelist Item with id '%s' not found.", id));
		}
		return itemOptional.get();
	}

	public WhitelistItem create(WhitelistItem whitelistItem) {
		return repository.save(whitelistItem);
	}

	public WhitelistItem update(WhitelistItem whitelistItem) {
		return repository.save(whitelistItem);
	}

	public void delete(WhitelistItem item) {
		repository.delete(item);
	}
}
