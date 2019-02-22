#!/usr/bin/groovy
package io.abc;
def build(name) {
  def scannerHome = tool 'mvn'
  sh "${scannerHome}/bin/mvn ${name}"
}
return this
