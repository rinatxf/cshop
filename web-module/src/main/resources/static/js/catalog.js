(function(){

  $(".catalog-item-img").mouseover(function(){
    $(this).find(".card-img-overlay").css("display","block");
  });

   $(".card-img-overlay").mouseleave(function(){
     $(this).css("display","none");
   });

   $(".card").mouseleave(function(){
        $(this).find(".card-img-overlay").css("display","none");
   });
})();

(function() {
    var xmlhttp = new XMLHttpRequest();

    $(document).on("submit", "#formCartAdd", function() {
        var params = $(this).serialize();
        var url = $(this).attr("action") + "?" + params;
        xmlhttp.onreadystatechange = function() {
            if(xmlhttp.status == 200 && xmlhttp.readyState == 4) {
                $(".contentCart").empty();
                $(".contentCart").append($(xmlhttp.responseText).find(".contentCart").html());
            }
        }
        xmlhttp.open("POST", url, true);
        xmlhttp.send();

        return false;
    });
})();