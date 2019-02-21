#!/usr/bin/groovy
package onjection.pipelines;

def build() {
  mvn clean deploy -U
}
def test(name) {
  sh "/usr/local/bin/${name}"
}
