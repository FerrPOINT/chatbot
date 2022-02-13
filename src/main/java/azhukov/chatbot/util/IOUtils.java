package azhukov.chatbot.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.function.Consumer;

@UtilityClass
@Slf4j
public final class IOUtils {

    public static void listFilesFromResources(String folder, String postfix, Consumer<InputStream> acceptor) {
        try {
            // Get all the files under this inner resource folder
            String scannedPackage = folder + "/*";
            PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
            Resource[] resources = scanner.getResources(scannedPackage);

            if (resources.length == 0)
                throw new RuntimeException("Could not find any resources in this scanned package: " + scannedPackage);
            else {
                for (Resource resource : resources) {
                    if (resource.getFilename().endsWith(postfix)) {
                        acceptor.accept(resource.getInputStream());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the resources folder: " + folder, e);
        }
    }

}
