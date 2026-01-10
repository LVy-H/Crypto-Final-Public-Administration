{
  description = "Crypto Final Public Administration Dev Environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
        # Prioritize JDK 25 (Early Access / Beta) if available, otherwise latest.
        java = pkgs.jdk25 or pkgs.jdk23 or pkgs.jdk;

        antigravityWithExtensions = pkgs.vscode-with-extensions.override {
          vscode = pkgs.antigravity;
          vscodeExtensions = with pkgs.vscode-extensions; [
            docker.docker
            redhat.java
            mathiasfrohlich.kotlin
            ms-kubernetes-tools.vscode-kubernetes-tools
            redhat.vscode-yaml
          ];
        };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            java
            gradle
            kotlin
            
            # Antigravity IDE with extensions
            antigravityWithExtensions

            # Infrastructure
            k3s
            kubernetes
            kubectl
            kubernetes-helm
            minio-client
            redis
            postgresql
          ];

          shellHook = ''
            echo "🚀 Crypto PQC Platform Dev Environment"
            echo "Java Version: $(java -version 2>&1 | head -n 1)"
            echo "Gradle Version: $(gradle -version 2>&1 | head -n 1)"
            export JAVA_HOME=${java}
          '';
        };
      }
    );
}
