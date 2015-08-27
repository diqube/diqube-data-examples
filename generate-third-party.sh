#!/bin/bash
#
# diqube: Distributed Query Base.
#
# Copyright (C) 2015 Bastian Gloeckle
#
# This file is part of diqube data examples.
#
# diqube data examples are free software: you can redistribute it
# and/or modify it under the terms of the GNU Affero General Public
# License as published by the Free Software Foundation, either
# version 3 of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#


cd "$(dirname "$0")"
rm THIRD-PARTY.txt
mvn -Pgenerate-third-party clean generate-resources
cat build/third-party-manual.txt >> THIRD-PARTY.txt
cd -