package dev.lab.mechanics;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ListFactoryTest {

    @Test
    void listOf_given_a_single_list_argument() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        //List.of(List<T>) returns List<List<T>>.
        //It's easy to mistakenly believe that we are creating a List<T> when "var" is specified as return variable.
        var result = List.of(source);

        assertThat(result).hasSize(1);
    }

    @Test
    void listOf_given_a_single_list_argument_aliases_the_source() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        var wrappedListOfList = List.of(source);

        source.add("c");

        assertThat(wrappedListOfList.getFirst()).isSameAs(source);
        assertThat(wrappedListOfList.getFirst()).containsExactly("a", "b", "c");

        //outer list is unmodifiable
        assertThatThrownBy(() -> wrappedListOfList.add(List.of()))
        .isInstanceOf(UnsupportedOperationException.class);

        //but inner list is modifiable
        wrappedListOfList.getFirst().add("d");
        assertThat(source).containsExactly("a", "b", "c", "d");
    }

    @Test
    void unmodifiableList_is_a_view_not_a_copy() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        List<String> view = Collections.unmodifiableList(source);

        //cannot change view
        assertThatThrownBy(() -> view.add("c"))
                .isInstanceOf(UnsupportedOperationException.class);

        //can change source, and it is visible from the view
        source.add("c");
        assertThat(view).containsExactly("a", "b", "c");

        List<String> copiedView = List.copyOf(view);
        assertThat(copiedView).isNotSameAs(view);

        source.add("d");
        assertThat(copiedView).containsExactly("a", "b", "c");
    }

    @Test
    void copyOf_is_independent_of_the_source_and_rejects_mutation() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        List<String> copied = List.copyOf(source);

        source.add("c");

        assertThat(source).containsExactly("a", "b", "c");
        assertThat(copied).containsExactly("a", "b");

        assertThatThrownBy(() -> copied.add("d"))
                .isInstanceOf(UnsupportedOperationException.class);

    }
}
