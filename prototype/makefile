SHELL := /bin/bash

MAVEN_VERSION := 3.9.9
MAVEN_ARCHIVE := apache-maven-$(MAVEN_VERSION)-bin.tar.gz
MAVEN_URL := https://archive.apache.org/dist/maven/maven-3/$(MAVEN_VERSION)/binaries/$(MAVEN_ARCHIVE)
MAVEN_ROOT := .maven
MAVEN_HOME := $(MAVEN_ROOT)/apache-maven-$(MAVEN_VERSION)
MVN := $(MAVEN_HOME)/bin/mvn
JAVA := java
MAIN_CLASS := UnclesPC.UnclesPCApplication

.PHONY: env compile run clean distclean

env:
	@mkdir -p $(MAVEN_ROOT)
	@if [ ! -x "$(MVN)" ]; then \
		echo "Bootstrapping Maven $(MAVEN_VERSION) into $(MAVEN_ROOT)"; \
		curl -fsSL "$(MAVEN_URL)" -o "$(MAVEN_ROOT)/$(MAVEN_ARCHIVE)"; \
		tar -xzf "$(MAVEN_ROOT)/$(MAVEN_ARCHIVE)" -C "$(MAVEN_ROOT)"; \
		rm -f "$(MAVEN_ROOT)/$(MAVEN_ARCHIVE)"; \
	fi
	@mkdir -p bin

compile: env
	@$(MVN) -q compile

run: compile
	@$(JAVA) -cp bin $(MAIN_CLASS)

clean:
	@find bin -type f -delete 2>/dev/null || true
	@find bin -depth -type d -empty -delete 2>/dev/null || true
	@find target -type f -delete 2>/dev/null || true
	@find target -depth -type d -empty -delete 2>/dev/null || true

distclean: clean
	@find $(MAVEN_ROOT) -type f -delete 2>/dev/null || true
	@find $(MAVEN_ROOT) -type l -delete 2>/dev/null || true
	@find $(MAVEN_ROOT) -depth -type d -empty -delete 2>/dev/null || true
