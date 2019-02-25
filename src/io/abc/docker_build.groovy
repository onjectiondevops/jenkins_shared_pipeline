#!/usr/bin/groovy
package io.abc;
def docker_build(name) {
	app = docker.build("${name}")
}
return this
