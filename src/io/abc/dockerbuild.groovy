#!/usr/bin/groovy
package io.abc;
def dockerbuild(name) {
	app = docker.build("${name}") 
}
return this
