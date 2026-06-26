package org.example.cloudstorage.helper;

import org.example.cloudstorage.exception.InvalidPathException;

public class PathValidationHelper {

    public static void ifPathInvalidThrowException(String path) {
        if (path.isBlank() || path.contains("..")) {
            throw new InvalidPathException("Path must not be blank or contain '..'");
        }
    }

    public static void ifPathInvalidThrowException(String from, String to) {
        if (from.isBlank()
                || from.contains("..")
                || to.isBlank()
                || to.contains("..")
                || from.equals(to)
                || (from.endsWith("/") && !to.endsWith("/"))
                || (to.endsWith("/") && !from.endsWith("/"))) {
            throw new InvalidPathException("Invalid path to or from");
        }
    }
}
