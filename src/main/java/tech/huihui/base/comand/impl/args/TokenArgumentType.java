package tech.huihui.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TokenArgumentType implements ArgumentType<String> {
   public static TokenArgumentType create() {
      return new TokenArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      while (reader.canRead() && reader.peek() != ' ') {
         reader.skip();
      }
      if (reader.getCursor() == start) {
         throw new CommandSyntaxException((CommandExceptionType)null, () -> {
            return "Пустой аргумент";
         });
      }
      return reader.getString().substring(start, reader.getCursor());
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}