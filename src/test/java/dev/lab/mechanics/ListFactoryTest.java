package dev.lab.mechanics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ListFactoryTest {

    @Test
    void listOf_given_a_single_list_argument() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        var result = List.of(source);

        assertThat(result).hasSize(2);
    }
}