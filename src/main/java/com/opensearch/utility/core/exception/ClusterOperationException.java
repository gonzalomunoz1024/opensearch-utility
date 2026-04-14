package com.opensearch.utility.core.exception;

public class ClusterOperationException extends OpenSearchUtilityException {

    public ClusterOperationException(String message) {
        super(message, "CLUSTER_OPERATION_ERROR");
    }

    public ClusterOperationException(String message, Throwable cause) {
        super(message, "CLUSTER_OPERATION_ERROR", cause);
    }

    public ClusterOperationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public static class ClusterUnreachableException extends ClusterOperationException {
        public ClusterUnreachableException(String clusterUrl) {
            super("Cluster unreachable: " + clusterUrl, "CLUSTER_UNREACHABLE");
        }

        public ClusterUnreachableException(String clusterUrl, Throwable cause) {
            super("Cluster unreachable: " + clusterUrl, cause);
        }
    }

    public static class ClusterHealthException extends ClusterOperationException {
        public ClusterHealthException(String message) {
            super(message, "CLUSTER_HEALTH_ERROR");
        }
    }
}
