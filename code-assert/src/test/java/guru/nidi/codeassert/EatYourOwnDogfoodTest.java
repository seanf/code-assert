/*
 * Copyright (C) 2015 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.codeassert;

import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;
import guru.nidi.codeassert.dependency.DependencyRule;
import guru.nidi.codeassert.dependency.DependencyRuler;
import guru.nidi.codeassert.dependency.DependencyRules;
import guru.nidi.codeassert.findbugs.BugCollector;
import guru.nidi.codeassert.findbugs.FindBugsAnalyzer;
import guru.nidi.codeassert.findbugs.FindBugsResult;
import guru.nidi.codeassert.jacoco.Coverage;
import guru.nidi.codeassert.junit.CodeAssertTest;
import guru.nidi.codeassert.model.ModelAnalyzer;
import guru.nidi.codeassert.model.ModelResult;
import guru.nidi.codeassert.pmd.*;
import net.sourceforge.pmd.RulePriority;
import org.junit.Test;

import static guru.nidi.codeassert.dependency.DependencyRules.denyAll;
import static guru.nidi.codeassert.junit.CodeAssertMatchers.packagesMatchExactly;
import static guru.nidi.codeassert.pmd.Rulesets.*;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class EatYourOwnDogfoodTest extends CodeAssertTest {
    @Test
    public void dependency() {
        System.gc();
        class GuruNidiCodeassert extends DependencyRuler {
            DependencyRule $self, config, dependency, findbugs, model, pmd, util, junit, jacoco;

            @Override
            public void defineRules() {
                config.mayUse(util);
                dependency.mayUse($self, util, config, model);
                findbugs.mayUse($self, util, config);
                model.mayUse($self, util, config);
                pmd.mayUse($self, util, config);
                jacoco.mayUse($self, util, config);
                util.mayUse($self);
                junit.mayUse($self, model, dependency, findbugs, pmd, jacoco);
            }
        }
        final DependencyRules rules = denyAll()
                .withExternals("edu*", "java*", "net*", "org*")
                .withRelativeRules(new GuruNidiCodeassert());
        assertThat(modelResult(), packagesMatchExactly(rules));
    }

    @Override
    protected ModelResult analyzeModel() {
        System.gc();
        return new ModelAnalyzer(AnalyzerConfig.maven().main()).analyze();
    }

    @Override
    protected FindBugsResult analyzeFindBugs() {
        System.gc();
        final BugCollector bugCollector = new BugCollector().just(
                In.locs("DependencyRules#withRules", "Ruleset").ignore("DP_DO_INSIDE_DO_PRIVILEGED"),
                In.loc("*Comparator").ignore("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE"),
                In.loc("*Exception").ignore("SE_BAD_FIELD"),
                In.clazz(Coverage.class).ignore("EQ_COMPARETO_USE_OBJECT_EQUALS"),
                In.everywhere().ignore("EI_EXPOSE_REP", "EI_EXPOSE_REP2", "SBSC_USE_STRINGBUFFER_CONCATENATION"),
                In.locs("ClassFileParser", "Constant", "MemberInfo", "Rulesets", "Reason").ignore("URF_UNREAD_FIELD"));
        return new FindBugsAnalyzer(AnalyzerConfig.maven().main(), bugCollector).analyze();
    }

    @Override
    protected PmdResult analyzePmd() {
        System.gc();
        final ViolationCollector collector = new ViolationCollector().minPriority(RulePriority.MEDIUM).just(
                In.everywhere().ignore(
                        "MethodArgumentCouldBeFinal", "AvoidFieldNameMatchingMethodName",
                        "CommentDefaultAccessModifier", "AbstractNaming", "AvoidFieldNameMatchingTypeName",
                        "UncommentedEmptyConstructor", "AvoidInstantiatingObjectsInLoops",
                        "UseStringBufferForStringAppends", "AvoidSynchronizedAtMethodLevel",
                        "JUnitAssertionsShouldIncludeMessage", "SimplifyStartsWith", "UncommentedEmptyMethodBody",
                        "ArrayIsStoredDirectly", "MethodReturnsInternalArray"),
                In.locs("AttributeInfo", "ConstantPool").ignore("ArrayIsStoredDirectly"),
                In.loc("SignatureParser").ignore("SwitchStmtsShouldHaveDefault"),
                In.clazz(Rulesets.class).ignore("TooManyMethods", "AvoidDuplicateLiterals"),
                In.loc("*Test").ignore("TooManyStaticImports"),
                In.loc("Reason").ignore("SingularField"),
                In.clazz(Coverage.class).ignore("ExcessiveParameterList"),
                In.locs("DependencyRules", "JavaClassImportBuilder").ignore("GodClass"));
        return new PmdAnalyzer(AnalyzerConfig.maven().main(), collector)
                .withRuleSets(basic(), braces(),
                        comments().maxLines(35).maxLineLen(100).requirement(Comments.Requirement.Ignored),
                        codesize().excessiveMethodLength(40).tooManyMethods(30),
                        design(), empty(), exceptions(), imports(), junit(),
                        naming().variableLen(1, 20).methodLen(2),
                        optimizations(), strings(),
                        sunSecure(), typeResolution(), unnecessary(), unused())
                .analyze();
    }

    @Override
    protected CpdResult analyzeCpd() {
        System.gc();
        final MatchCollector collector = new MatchCollector()
                .just(In.everywhere().ignore("public boolean equals(Object o)", "public int hashCode()", "public String toString()"));

        return new CpdAnalyzer(AnalyzerConfig.maven().main(), 27, collector).analyze();
    }
}
