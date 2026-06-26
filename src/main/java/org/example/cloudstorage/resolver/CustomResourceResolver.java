package org.example.cloudstorage.resolver;

import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

public class CustomResourceResolver extends PathResourceResolver {

    @Override
    @Nullable
    protected org.springframework.core.io.Resource getResource(
            @NonNull String resourcePath, org.springframework.core.io.@NonNull Resource location)
            throws IOException {
        var resource = super.getResource(resourcePath, location);
        if (resource == null) {
            return new ClassPathResource("/static/index.html");
        }
        return resource;
    }
}
