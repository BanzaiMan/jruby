#!/usr/bin/env ruby

require 'erb'

# fake node.java.default_version
node = Object.new
def node.java
  j=Object.new
  def j.default_version
    'oraclejdk7'
  end
  j
end

`curl -O https://raw.githubusercontent.com/travis-ci/travis-cookbooks/f6cedf1930952a120e14666eaf78d8e29b08ee08/ci_environment/java/templates/ubuntu/jdk_switcher.sh.erb`

template = File.read 'jdk_switcher.sh.erb'

f = File.write("#{ENV['HOME']}/.jdk_switcher_rc", ERB.new(template).result(binding))
