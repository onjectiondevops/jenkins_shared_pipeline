#!/usr/bin/groovy
package io.abc;
def build() {
  mvn clean deploy -U
}
def test(name) {
  sh "/usr/local/bin/${name}"
}
return this
