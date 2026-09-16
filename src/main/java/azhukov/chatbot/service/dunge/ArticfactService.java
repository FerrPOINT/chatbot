package azhukov.chatbot.service.dunge;

import azhukov.chatbot.service.dunge.data.Artifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Compatibility facade for old call sites. New domain code uses {@link ArtifactCatalog}. */
@Service
@RequiredArgsConstructor
public class ArticfactService {
    private final ArtifactCatalog catalog;

    public Artifact getById(String id) { return catalog.get(id); }
    public List<Artifact> getItems() { return catalog.all(); }
}
