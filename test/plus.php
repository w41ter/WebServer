<!doctype html5>
<html>
<head>
<title>a + b</title>
<script>
    function plus() {
        var a = document.getElementById("a").value;
        var b = document.getElementById("b").value;
        var goto = "./plus.php?a=" + a + "&b=" + b;
        window.location = goto;
    }
</script>
</head>
<body>
<center>
    <div>c = <spin><?php echo $_GET['a'] + $_GET['b']; ?></spin></div>
    <br />
    <div>a = <input id="a" type="text" value="<?php echo $_GET['a'] ?>"></input></div>
    <br />
    <div>b = <input id="b" type="text" value="<?php echo $_GET['b'] ?>"></input></div>
    <br />
    <div><button onclick="plus()">submit</button></div>
</center>
</body>
</html>